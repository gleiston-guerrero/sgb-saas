package com.uteq.backend.scheduling;

import com.uteq.backend.entity.StatusReservation;
import com.uteq.backend.entity.Reservation;
import com.uteq.backend.repository.StatusReservationRepository;
import com.uteq.backend.repository.ReservationProcedureRepository;
import com.uteq.backend.repository.ReservationRepository;
import com.uteq.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Job periódico que expira en lote las reservaciones vencidas no retiradas.
 * Notifica cada una antes del UPDATE masivo; corre en una sola instancia.
 *
 * El ciclo es tolerante a seed parcial: si falta alguna fila de catálogo
 * o falla el SP, registra un WARN y omite el ciclo en vez de lanzar (un
 * job periódico no debe spamear ERROR cada 15 min por dato ausente).
 * Ver tests en ReservationSchedulerTest.
 */
@Component
@ConditionalOnProperty(name = "sgb.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);
    private static final List<String> ESTADOS_RESERVA_POR_EXPIRAR = List.of("PENDIENTE", "LISTA_PARA_RETIRO");

    private final ReservationProcedureRepository reservationProcedureRepository;
    private final ReservationRepository reservationRepository;
    private final StatusReservationRepository statusReservationRepository;
    private final NotificationService notificationService;

    public ReservationScheduler(ReservationProcedureRepository reservationProcedureRepository,
                                ReservationRepository reservationRepository,
                                StatusReservationRepository statusReservationRepository,
                                NotificationService notificationService) {
        this.reservationProcedureRepository = reservationProcedureRepository;
        this.reservationRepository = reservationRepository;
        this.statusReservationRepository = statusReservationRepository;
        this.notificationService = notificationService;
    }

    // Cada 15 minutos -- valor que podra ser modificado mas adelante si es necesario.
    // initialDelay de 60s para no competir con Flyway/seed en el arranque
    // (antes el primer disparo era inmediato y fallaba en contexto de test
    // con H2 vacío o en Postgres antes de terminar el seed).
    /**
     * Handles expirar reservations Vencidas.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 60 * 1000)
    public void expireOverdueReservations() {
        List<Integer> statusIds = ESTADOS_RESERVA_POR_EXPIRAR.stream()
                .map(this::idStatus)
                .flatMap(Optional::stream)
                .toList();
        if (statusIds.isEmpty()) {
            log.warn("Job de expiración de reservaciones omitido: catálogo estados_reservacion sin filas {}",
                    ESTADOS_RESERVA_POR_EXPIRAR);
            return;
        }

        notifyExpiringSoon(statusIds);

        try {
            Integer rowsUpdated = reservationProcedureRepository.spExpireReservationsVencidasProcedure();
            log.info("Job de expiración de reservaciones: {} filas actualizadas", rowsUpdated);
        } catch (DataAccessException ex) {
            log.warn("Job de expiración de reservaciones omitido: falló sp_expirar_reservaciones_vencidas ({})",
                    ex.getMessage());
        }
    }

    private void notifyExpiringSoon(List<Integer> statusIds) {

        List<Reservation> byExpire = reservationRepository
                .findByStatusReservationIdInAndDateLimitPickupBefore(statusIds, OffsetDateTime.now());

        for (Reservation reservation : byExpire) {
            notificationService.notifyReservationExpired(reservation);
        }
    }

    private Optional<Integer> idStatus(String name) {
        return statusReservationRepository.findByName(name)
                .map(StatusReservation::getId)
                .or(() -> {
                    log.warn("Catálogo estados_reservacion sin fila '{}': se ignora en este ciclo", name);
                    return Optional.empty();
                });
    }
}