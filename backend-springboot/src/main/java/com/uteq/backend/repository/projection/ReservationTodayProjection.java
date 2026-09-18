package com.uteq.backend.repository.projection;



/**
 * Proyección de una fila de reservaciones próximas al vencimiento
 * (JPQL en {@code ReservationRepository}, P5).
 * Resuelve libro/usuario en una sola query para evitar el N+1 que
 * tendría el frontend pidiendo cada libro/usuario por separado.
 */
public interface ReservationTodayProjection {
    /** Identificador de la reservación. */
    Long getReservationId();
    /** Nombre completo del lector. */
    String getUserName();
    /** Correo del lector. */
    String getUserEmail();
    /** Título del libro reservado. */
    String getBookTitle();
    /** ISBN del libro reservado. */
    String getBookIsbn();
    /** Nombre del estado de la reservación. */
    String getStatusName();
    /** Límite de retiro (OffsetDateTime del atributo; el service normaliza a UTC). */
    java.time.OffsetDateTime getDateLimitPickup();
}
