package com.uteq.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Política de la cookie refreshToken (P5-regresión §1: la cookie de
 * producción es literalmente {@code Secure + SameSite=None}).
 *
 * <p>El bean por defecto (todos los perfiles incluido prod) emite la
 * cookie dura: no existe ninguna propiedad ni variable capaz de
 * degradarla en despliegue. La excepción HTTP local vive únicamente en
 * el perfil explícito {@code dev-local-http} (bean separado): activar
 * ese perfil en producción sería un cambio de despliegue visible, nunca
 * un flag silencioso.
 */
@Configuration
public class RefreshCookieConfig {

    /** Política de construcción de la cookie refreshToken. */
    public interface RefreshCookiePolicy {
        ResponseCookie build(String name, String value, Duration maxAge);
    }

    /**
     * Política productiva: siempre Secure + SameSite=None (cross-site
     * Render), HttpOnly, path {@code /api/auth}.
     *
     * @return política dura incondicional
     */
    @Bean
    @Profile("!dev-local-http")
    public RefreshCookiePolicy refreshCookiePolicy() {
        return (name, value, maxAge) -> ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    /**
     * Excepción solo-dev-local-http: sin Secure + SameSite=Lax (None
     * exige Secure por especificación y el navegador descarta la cookie
     * Secure en http:// sin TLS, rompiendo el refresh local).
     *
     * @return política relajada solo para desarrollo local
     */
    @Bean
    @Profile("dev-local-http")
    public RefreshCookiePolicy refreshCookiePolicyDevLocalHttp() {
        return (name, value, maxAge) -> ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
