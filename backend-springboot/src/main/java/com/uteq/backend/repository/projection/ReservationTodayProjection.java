package com.uteq.backend.repository.projection;



/**
 * Proyección de una fila retornada por la query nativa
 * {@code buscarReservacionesDeHoy()} en {@code ReservacionRepository}.
 * Resuelve libro/usuario en una sola query para evitar el N+1 que
 * tendría el frontend pidiendo cada libro/usuario por separado.
 */
public interface ReservationTodayProjection {
    Long getReservationId();
    String getUserName();
    String getUserEmail();
    String getBookTitle();
    String getBookIsbn();
    String getStatusName();
    // OffsetDateTime (tipo del atributo): Spring no convierte
    // OffsetDateTime->Instant en proyecciones; el service normaliza a UTC.
    java.time.OffsetDateTime getDateLimitPickup();
}
