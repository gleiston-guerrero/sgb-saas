package com.uteq.backend.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Contador de intentos fallidos de login en Redis. Clave por correo+IP
 * para no bloquear al dueño por intentos ajenos; ante caída de Redis
 * degrada a fail-open sin romper el login.
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private static final String KEY_PREFIX = "login-attempts:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.security.login.max-attempts}")
    private int maxAttempts;

    @Value("${app.security.login.rate-limit-window-seconds}")
    private long rateLimitWindowSeconds;

    /**
     * Indica si el par correo e IP alcanzó el máximo de intentos fallidos en la ventana vigente.
     * Degrada a sin bloqueo si Redis no responde.
     *
     * @param email correo usado en el intento de login
     * @param ip dirección IP del intento de login
     * @return true si está bloqueado; false en caso contrario o si Redis no responde
     */

    public boolean isBlocked(String email, String ip) {
        try {
            String value = redisTemplate.opsForValue().get(key(email, ip));
            return value != null && Long.parseLong(value) >= maxAttempts;
        } catch (DataAccessException e) {
            log.warn("Redis no disponible en estaBloqueado (fail-open, sin bloqueo): correo={}", email, e);
            return false;
        }
    }

    /**
     * Registra un intento fallido: incrementa el contador del par correo e IP y fija
     * la expiración de la ventana solo en el primer intento.
     *
     * @param email correo usado en el intento fallido
     * @param ip dirección IP del intento fallido
     */
    public void registerFailure(String email, String ip) {
        try {
            String key = key(email, ip);
            Long freshValue = redisTemplate.opsForValue().increment(key);
            if (freshValue != null && freshValue == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(rateLimitWindowSeconds));
            }
        } catch (DataAccessException e) {
            log.warn("Redis no disponible en registrarFallo (contador no incrementado): correo={}", email, e);
        }
    }

    /**
     * Limpia el contador de intentos fallidos tras un login exitoso.
     *
     * @param email correo cuyo contador se limpia
     * @param ip dirección IP cuyo contador se limpia
     */

    public void reset(String email, String ip) {
        try {
            redisTemplate.delete(key(email, ip));
        } catch (DataAccessException e) {
            log.warn("Redis no disponible en resetear (contador no limpiado): correo={}", email, e);
        }
    }

    /**
     * Devuelve los segundos restantes de la ventana de bloqueo del par correo e IP.
     * Si no hay TTL o Redis no responde, informa la ventana completa configurada.
     *
     * @param email correo a consultar
     * @param ip dirección IP a consultar
     * @return segundos restantes de bloqueo
     */
    public long remainingSeconds(String email, String ip) {
        try {
            Long ttl = redisTemplate.getExpire(key(email, ip));
            return (ttl == null || ttl < 0) ? rateLimitWindowSeconds : ttl;
        } catch (DataAccessException e) {
            log.warn("Redis no disponible en segundosRestantes (se informa ventana completa): correo={}", email, e);
            return rateLimitWindowSeconds;
        }
    }

    private String key(String email, String ip) {
        return KEY_PREFIX + email + ":" + ip;
    }
}
