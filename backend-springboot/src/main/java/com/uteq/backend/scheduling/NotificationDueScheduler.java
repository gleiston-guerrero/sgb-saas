package com.uteq.backend.scheduling;

import com.uteq.backend.entity.StatusLoan;
import com.uteq.backend.entity.Loan;
import com.uteq.backend.repository.StatusLoanRepository;
import com.uteq.backend.repository.LoanRepository;
import com.uteq.backend.service.ConfigurationSystemService;
import com.uteq.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Job periódico que alerta préstamos vigentes próximos a vencer.
 * La deduplicación vive en NotificacionService, no aquí.
 *
 * El ciclo es tolerante a seed parcial: si falta la clave de
 * configuración o alguna fila de catálogo, registra un WARN y omite el
 * ciclo en vez de lanzar (un job periódico no debe spamear ERROR cada
 * minuto por dato ausente). Ver tests en NotificationDueSchedulerTest.
 */
@Component
@ConditionalOnProperty(name = "sgb.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationDueScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDueScheduler.class);
    private static final List<String> ESTADOS_PRESTAMO_VIGENTE = List.of("ACTIVO", "RENOVADO");

    private final LoanRepository loanRepo;
    private final StatusLoanRepository statusLoanRepo;
    private final NotificationService notificationService;
    private final ConfigurationSystemService configurationSystemService;

    /**
     * Crea el job con los repositorios de préstamos y estados, más los servicios
     * de notificaciones y de configuración del sistema.
     *
     * @param loanRepo repositorio para buscar préstamos por vencer
     * @param statusLoanRepo repositorio para resolver los estados vigentes
     * @param notificationService servicio que genera la alerta de vencimiento
     * @param configurationSystemService servicio que lee los días de anticipación
     */
    public NotificationDueScheduler(LoanRepository loanRepo,
                                             StatusLoanRepository statusLoanRepo,
                                             NotificationService notificationService,
                                             ConfigurationSystemService configurationSystemService) {
        this.loanRepo = loanRepo;
        this.statusLoanRepo = statusLoanRepo;
        this.notificationService = notificationService;
        this.configurationSystemService = configurationSystemService;
    }
    /**
     * Job cada minuto que alerta los préstamos vigentes próximos a vencer.
     * Lee la ventana en días desde la configuración, busca los préstamos con devolución
     * estimada dentro de la ventana y genera una alerta por cada uno. Omite el ciclo
     * con un aviso si falta la configuración o el catálogo de estados.
     */
    @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
    public void notifyNextToExpire() {
        final int daysAnticipacion;
        try {
            daysAnticipacion = configurationSystemService.getIntegerValue("dias_anticipacion_vencimiento");
        } catch (RuntimeException ex) {
            log.warn("Job de notificación de vencimiento omitido: clave 'dias_anticipacion_vencimiento' ausente o inválida ({})",
                    ex.getMessage());
            return;
        }
        int minutesAnticipacion = daysAnticipacion * 24 * 60;

        List<Integer> statusIds = ESTADOS_PRESTAMO_VIGENTE.stream()
                .map(this::idStatus)
                .flatMap(Optional::stream)
                .toList();
        if (statusIds.isEmpty()) {
            log.warn("Job de notificación de vencimiento omitido: catálogo estados_prestamo sin filas {}",
                    ESTADOS_PRESTAMO_VIGENTE);
            return;
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime limit = ahora.plusMinutes(minutesAnticipacion);

        List<Loan> nextsAExpire = loanRepo
                .findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(statusIds, ahora, limit);

        for (Loan loan : nextsAExpire) {
            notificationService.generateDueAlert(loan);
        }

        log.info("Job de notificación de vencimiento: {} préstamos evaluados (ventana: {} días)", nextsAExpire.size(), daysAnticipacion);
    }

    private Optional<Integer> idStatus(String name) {
        return statusLoanRepo.findByName(name)
                .map(StatusLoan::getId)
                .or(() -> {
                    log.warn("Catálogo estados_prestamo sin fila '{}': se ignora en este ciclo", name);
                    return Optional.empty();
                });
    }
}
