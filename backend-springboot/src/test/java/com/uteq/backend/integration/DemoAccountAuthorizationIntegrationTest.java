package com.uteq.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidencia literal de P10 con cero mocks: login real de la cuenta demo,
 * JWT decodificado con rol {@code LECTOR} y 403 reales contra recurso
 * prohibido por rol y contra recurso ajeno.
 *
 * <p>Todo es infraestructura real: PostgreSQL y Redis en Testcontainers,
 * migraciones Flyway versionadas, cadena de seguridad completa
 * (sin {@code addFilters = false}) y servicios sin mocks.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class DemoAccountAuthorizationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        java.net.URL classLocation = DemoAccountAuthorizationIntegrationTest.class
                .getProtectionDomain().getCodeSource().getLocation();
        java.nio.file.Path migrationsDir;
        try {
            migrationsDir = java.nio.file.Paths.get(classLocation.toURI())
                    .getParent()
                    .getParent()
                    .getParent()
                    .resolve("database/migrations");
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Could not resolve migrations path", e);
        }
        String migrationsPath = "filesystem:" + migrationsDir.toAbsolutePath();
        registry.add("spring.flyway.locations",
                () -> "classpath:db/test-migrations," + migrationsPath);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String loginDemo() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"u@uteq.edu.ec\",\"password\":\"usuario1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cuerpo = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = cuerpo.get("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    void loginDemo_jwtDecodificadoTraeRolLector() throws Exception {
        String token = loginDemo();

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("correo", String.class)).isEqualTo("u@uteq.edu.ec");
        assertThat(claims.get("rol", String.class)).isEqualTo("LECTOR");
        List<String> roles = new ArrayList<>();
        claims.get("roles", List.class).forEach(r -> roles.add(String.valueOf(r)));
        assertThat(roles).containsExactly("LECTOR");
    }

    @Test
    void lectorAEndpointSoloAdmin_devuelve403() throws Exception {
        String token = loginDemo();

        mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lectorAMultasDeOtroUsuario_devuelve403() throws Exception {
        String token = loginDemo();
        Long demoId = jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE correo = 'u@uteq.edu.ec'", Long.class);
        assertThat(demoId).isNotNull();
        // validateAccessUser compara IDs antes de consultar: el ajeno no
        // necesita existir como fixture para que el 403 sea real.
        long ajenoId = demoId + 999999L;

        mockMvc.perform(get("/api/v1/multas/usuario/{id}", ajenoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
