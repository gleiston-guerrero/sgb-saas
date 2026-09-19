package com.uteq.backend.repository.projection;



/**
 * Proyección de una fila de reservaciones próximas al vencimiento
 * (JPQL en {@code ReservationRepository}, P5).
 * Resuelve libro/usuario en una sola query para evitar el N+1 que
 * tendría el frontend pidiendo cada libro/usuario por separado.
 */
public interface ReservationTodayProjection {
    /**
     * Identificador de la reservación.
     *
     * @return identificador persistente de la reservación.
     */
    Long getReservationId();
    /**
     * Nombre completo del lector.
     *
     * @return nombre visible del lector.
     */
    String getUserName();
    /**
     * Correo del lector.
     *
     * @return correo registrado del lector.
     */
    String getUserEmail();
    /**
     * Título del libro reservado.
     *
     * @return título del libro reservado.
     */
    String getBookTitle();
    /**
     * ISBN del libro reservado.
     *
     * @return ISBN del libro reservado.
     */
    String getBookIsbn();
    /**
     * Nombre del estado de la reservación.
     *
     * @return estado actual de la reservación.
     */
    String getStatusName();
    /**
     * Límite de retiro (OffsetDateTime del atributo; el service normaliza a UTC).
     *
     * @return fecha y hora límite para retirar el libro.
     */
    java.time.OffsetDateTime getDateLimitPickup();
}
