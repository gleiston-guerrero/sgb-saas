package com.uteq.backend.service;

import com.uteq.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Código de verificación de correo al registrarse: valor efímero en Redis
 * con TTL, sin tabla nueva en Postgres.
 */
@Service
@RequiredArgsConstructor
public class VerificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailService.class);
    private static final String KEY_PREFIX = "verificacion-correo:";
    private static final int LONGITUD_CODIGO = 6;

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.verificacion-correo.ttl-minutes}")
    private long ttlMinutes;

    /**
     * Genera un código aleatorio de 6 dígitos, lo guarda en Redis con el TTL configurado y lo envía
     * al correo del titular. Si Redis no responde el registro se interrumpe; si solo falla el correo,
     * el código queda guardado y el usuario puede pedir el reenvío.
     *
     * @param user usuario recién registrado que debe confirmar su correo
     * @throws ServiceTemporarilyNotAvailableException si Redis no acepta el guardado del código
     */
    public void generateAndSendCode(User user) {
        String code = generateCode();
        try {
            redisTemplate.opsForValue().set(key(user.getEmail()), code, Duration.ofMinutes(ttlMinutes));
        } catch (DataAccessException e) {
            log.error("Redis no disponible al generar código de verificación para {}", user.getEmail(), e);
            throw new ServiceTemporarilyNotAvailableException(
                    "El servicio de verificación de correo no está disponible temporalmente. Intente más tarde.");
        }

        String body = "<p>Hola " + user.getName() + ",</p>"
                + "<p>Tu código de verificación es: <b>" + code + "</b></p>"
                + "<p>Vence en " + ttlMinutes + " minutos.</p>";
        boolean enviado = emailService.sendEmail(user.getEmail(), "Verifica tu correo - SGB-SaaS", body);
        if (!enviado) {
            // Fallo de SMTP no rompe el registro; queda como deuda operativa (reenviar).
            log.warn("No se pudo enviar el código de verificación a {}", user.getEmail());
        }
    }

    /**
     * Valida el código de un solo uso contra el guardado en Redis y lo consume al acertar.
     * Rechaza códigos vencidos o nunca solicitados igual que los incorrectos.
     *
     * @param email correo pendiente de confirmación cuyo código se valida
     * @param codeIngresado código de 6 dígitos ingresado por el titular
     * @throws CodeVerificationInvalidException si el código expiró, es incorrecto o Redis no responde a la lectura
     */
    public void validate(String email, String codeIngresado) {
        String key = key(email);
        String codeAlmacenado;
        try {
            codeAlmacenado = redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            log.error("Redis no disponible al validar código de verificación para {}", email, e);
            throw new CodeVerificationInvalidException(
                    "El servicio de verificación no está disponible temporalmente. Intente más tarde.");
        }

        if (codeAlmacenado == null) {
            throw new CodeVerificationInvalidException(
                    "El código expiró o no se ha solicitado uno para este correo.");
        }
        if (!codeAlmacenado.equals(codeIngresado)) {
            throw new CodeVerificationInvalidException("El código ingresado es incorrecto.");
        }

        // Un solo uso: se borra apenas se valida.
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.warn("Redis no disponible al eliminar código ya validado para {}", email, e);
        }
    }

    private String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%0" + LONGITUD_CODIGO + "d", value);
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }
}
