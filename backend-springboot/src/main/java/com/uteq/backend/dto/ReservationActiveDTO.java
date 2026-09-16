package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reserva vigente de un usuario en la ventanilla de préstamos (GET
 * /api/v1/prestamos/gestion/reserva-activa?usuarioId=). "Vigente" = estado
 * PENDIENTE o LISTA_PARA_RETIRO (mismo criterio que PrestamoService para
 * bloquear renovaciones). Responde 404 con ProblemDetail si no existe, lo
 * que el frontend interpreta como "Caso B: préstamo directo".
 *
 * Los datos del libro viajan resueltos para que la tarjeta "Nuevo Préstamo"
 * no tenga que hacer N consultas extra.
 *
 * @param reservationId identificador de la reservación vigente
 * @param bookId identificador del libro reservado
 * @param title título del libro
 * @param authors nombres de los autores
 * @param isbn ISBN del libro
 * @param dateReservation fecha de la reservación
 * @param dateLimitPickup fecha límite de retiro
 * @param daysLoanSuggested días de préstamo sugeridos
 * @param yearPublication año de publicación
 * @param stockAvailable existencias disponibles
 * @param stockTotal existencias totales
 * @param locationPhysical ubicación física del ejemplar
 * @param categories nombres de las categorías
 * @param tieneCover si el libro tiene portada binaria
 */
public record ReservationActiveDTO( @JsonProperty("reservacionId") Long reservationId, @JsonProperty("libroId") Long bookId, @JsonProperty("titulo") String title, @JsonProperty("autores") List<String> authors,
        String isbn, @JsonProperty("fechaReserva") OffsetDateTime dateReservation, @JsonProperty("fechaLimiteRetiro") OffsetDateTime dateLimitPickup, @JsonProperty("diasPrestamoSugerido") Integer daysLoanSuggested, @JsonProperty("anioPublicacion") Short yearPublication, @JsonProperty("stockDisponible") Short stockAvailable,
        Short stockTotal,
        @JsonProperty("ubicacionFisica") String locationPhysical, @JsonProperty("categorias") List<String> categories, @JsonProperty("tienePortada") boolean tieneCover
) {}
