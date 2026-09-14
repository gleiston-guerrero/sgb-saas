package com.uteq.backend.integration;

import com.uteq.backend.dto.LoginRequestDTO;
import com.uteq.backend.dto.TokenResponseDTO;
import com.uteq.backend.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class DemoPublicAccountsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AuthService authService;

    @Value("${security.jwt.secret}")
    String jwtSecret;

    @Test
    void publicDemoAccounts_existWithExpectedRoles_and_lectorCanLogin() {
        // Users as documented in README: lector.demo@sgb-saas.local, bibliotecario.demo@sgb-saas.local, gerente.demo@sgb-saas.local
        List<String> lectorRoles = jdbcTemplate.queryForList("SELECT r.nombre FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id = u.id JOIN roles r ON r.id = ur.rol_id WHERE u.correo = 'lector.demo@sgb-saas.local' ORDER BY r.nombre", String.class);
        List<String> bibliRoles = jdbcTemplate.queryForList("SELECT r.nombre FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id = u.id JOIN roles r ON r.id = ur.rol_id WHERE u.correo = 'bibliotecario.demo@sgb-saas.local' ORDER BY r.nombre", String.class);
        List<String> gerenteRoles = jdbcTemplate.queryForList("SELECT r.nombre FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id = u.id JOIN roles r ON r.id = ur.rol_id WHERE u.correo = 'gerente.demo@sgb-saas.local' ORDER BY r.nombre", String.class);

        assertThat(lectorRoles).contains("LECTOR");
        assertThat(bibliRoles).contains("BIBLIOTECARIO");
        assertThat(gerenteRoles).contains("GERENTE");

        // Attempt login for lector demo (password from seed.sql / migrations)
        TokenResponseDTO tokens = authService.login(new LoginRequestDTO("lector.demo@sgb-saas.local", "lector.demo"), "127.0.0.1");

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(tokens.accessToken())
                .getPayload();

        assertThat(claims.get("correo", String.class)).isEqualTo("lector.demo@sgb-saas.local");
        assertThat(claims.get("rol", String.class)).isEqualTo("LECTOR");
    }
}
