package com.uteq.backend.repository;

import com.uteq.backend.entity.Reservation;
import com.uteq.backend.repository.projection.ReservationTodayProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CRUD elemental sobre {@code reservaciones}. La expiración masiva vive
 * en {@link ReservationProcedureRepositoryCustom#spExpireReservationsVencidasProcedure}.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Pagina las reservaciones del usuario dado. */
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    // Usado por PrestamoService.renovar(): una renovación se bloquea si OTRO
    // usuario (usuarioId <> el dueño del préstamo) tiene una reserva vigente
    // sobre el mismo libro. "Vigente" = no RETIRADA/EXPIRADA/CANCELADA, ver
    // los ids que arma PrestamoService a partir de EstadoReservacionRepository.
    /** Indica si otro usuario tiene reserva del libro dado en los estados dados. */
    boolean existsByBookIdAndStatusReservationIdInAndUserIdNot(
            Long bookId, List<Integer> statusesReservationIds, Long userId);

    // Reservaciones a expirar en la corrida actual (mismo filtro que la función masiva, para notificar antes del UPDATE).
    /** Lista las reservaciones en los estados dados cuya fecha límite ya pasó. */
    List<Reservation> findByStatusReservationIdInAndDateLimitPickupBefore(
            List<Integer> statusesReservationIds, OffsetDateTime ahora);

    // Reserva vigente más reciente del usuario (la que se convierte en préstamo).
    /** Busca la reserva más reciente del usuario en los estados dados. */
    Optional<Reservation> findFirstByUserIdAndStatusReservationIdInOrderByDateReservationDesc(
            Long userId, List<Integer> statusesReservationIds);

    // Conteo de reservas vigentes del usuario (badge de activas).
    /** Cuenta las reservaciones del usuario en los estados dados. */
    long countByUserIdAndStatusReservationIdIn(
            Long userId, List<Integer> statusesReservationIds);

    // Dashboard del bibliotecario: reservaciones cuya fecha límite de
    // retiro cae HOY, con libro/usuario ya resueltos (evita el N+1 que
    // tendría el frontend pidiendo cada libro/usuario por separado para
    // un widget que se carga en cada visita al dashboard).
    // Migradas a JPQL (P5): la ventana de fechas llega por parámetro desde
    // el service (determinista y portable) en vez de CURRENT_DATE +
    // INTERVAL de PostgreSQL. JOINs cartesianos porque Reservation expone
    // FK planas sin relaciones JPA (decisión arquitectónica).
    /**
     * Reservaciones PENDIENTE/LISTA_PARA_RETIRO con vencimiento en [start, end).
     *
     * @param start inicio de la ventana (inclusive)
     * @param end fin de la ventana (exclusive)
     * @return reservaciones ordenadas por fecha límite ascendente
     */
    @Query("""
        SELECT r.id AS reservationId,
               CONCAT(u.name, ' ', u.lastName) AS userName,
               u.email AS userEmail,
               b.title AS bookTitle,
               b.isbn AS bookIsbn,
               s.name AS statusName,
               r.dateLimitPickup AS dateLimitPickup
        FROM Reservation r, User u, Book b, StatusReservation s
        WHERE u.id = r.userId
          AND b.id = r.bookId
          AND s.id = r.statusReservationId
          AND r.dateLimitPickup >= :start
          AND r.dateLimitPickup < :end
          AND s.name IN ('PENDIENTE', 'LISTA_PARA_RETIRO')
        ORDER BY r.dateLimitPickup ASC
        """)
    List<ReservationTodayProjection> searchReservationsToday(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("""
        SELECT r.id AS reservationId,
               CONCAT(u.name, ' ', u.lastName) AS userName,
               u.email AS userEmail,
               b.title AS bookTitle,
               b.isbn AS bookIsbn,
               s.name AS statusName,
               r.dateLimitPickup AS dateLimitPickup
        FROM Reservation r, User u, Book b, StatusReservation s
        WHERE u.id = r.userId
          AND b.id = r.bookId
          AND s.id = r.statusReservationId
          AND r.dateLimitPickup >= :start
          AND s.name IN ('PENDIENTE', 'LISTA_PARA_RETIRO')
        ORDER BY r.dateLimitPickup ASC
        """)
    /**
     * Reservaciones PENDIENTE/LISTA_PARA_RETIRO con vencimiento desde start.
     *
     * @param start inicio de la ventana (inclusive)
     * @return reservaciones ordenadas por fecha límite ascendente
     */
    List<ReservationTodayProjection> searchReservationsNexts(
            @Param("start") OffsetDateTime start);
}
