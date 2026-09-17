package com.uteq.backend.scheduling;

import com.uteq.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Job periódico que elimina cuentas de usuarios cuyo correo no fue
 * verificado dentro de las últimas 24 horas. Sigue el patrón de
 * {@link ReservationScheduler}.
 */
@Component
@ConditionalOnProperty(name = "sgb.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class UserScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserScheduler.class);
    private static final long HORAS_EXPIRACION = 24;

    private final UserRepository userRepo;

    /**
     * Crea el job con el repositorio de usuarios.
     *
     * @param userRepo repositorio para eliminar cuentas no verificadas
     */
    public UserScheduler(UserRepository userRepo) {
        this.userRepo = userRepo;
    }
    /**
     * Job cada hora que elimina las cuentas no verificadas con más de 24 horas de antigüedad
     * (cada 1 hora, con 1 minuto de retardo inicial).
     */
    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void deleteNotVerifiedsOverdues() {
        Instant cutoff = Instant.now().minus(HORAS_EXPIRACION, ChronoUnit.HOURS);
        int eliminados = userRepo.deleteNotVerifiedsBefore(cutoff);
        if (eliminados > 0) {
            log.info("Auto-eliminación de usuarios no verificados: {} cuentas eliminadas", eliminados);
        }
    }
}
