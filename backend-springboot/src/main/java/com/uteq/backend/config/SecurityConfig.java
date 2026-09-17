package com.uteq.backend.config;

import com.uteq.backend.security.JwtAuthFilter;
import com.uteq.backend.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad stateless con JWT: define el codificador, el proveedor
 * de autenticación, la cadena de filtros y las reglas de acceso, CORS y cabeceras CSP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    /**
     * Crea el codificador de contraseñas BCrypt con fuerza 12.
     *
     * @return codificador de contraseñas para usuarios
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    /**
     * Expone el gestor de autenticación construido por Spring Security.
     *
     * @param config configuración de autenticación de Spring
     * @return gestor de autenticación del contexto
     * @throws Exception si no se puede construir el gestor
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    /**
     * Crea el proveedor de autenticación DAO con el servicio de usuarios y BCrypt.
     *
     * @return proveedor de autenticación por correo y contraseña
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsServiceImpl);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    /**
     * Construye la cadena de filtros stateless sin CSRF: deja públicas las rutas de
     * autenticación, catálogo público, docs y salud, exige autenticación en el resto,
     * aplica CORS y cabeceras CSP, e inserta el filtro JWT antes del filtro de usuario.
     *
     * @param http configuración HTTP de Spring Security
     * @return cadena de filtros de seguridad construida
     * @throws Exception si no se puede construir la cadena
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/registro",
                                "/api/auth/verificar-correo",
                                "/api/auth/reenviar-codigo",
                                "/api/auth/solicitar-reset",
                                "/api/auth/reset",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                // Portal público (Rama C): superficie de solo
                                // lectura del catálogo sin cuenta (ver
                                // PublicoLibroController). Angosta a propósito:
                                // solo /api/publico/libros, nada más.
                                "/api/publico/**",
                                "/api/docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        // CSP restrictiva: la única superficie HTML es Swagger UI.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self'; "
                                        + "script-src-attr 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data: blob:; "
                                        + "connect-src 'self' https://sgb-backend-b058.onrender.com; "
                                        + "frame-ancestors 'none'; "
                                        + "base-uri 'self'; "
                                        + "form-action 'none'; "
                                        + "object-src 'none'"
                        ))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "https://biblora-sgb.onrender.com"));
        // PATCH incluido: los endpoints PATCH mueren en el preflight sin él.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
