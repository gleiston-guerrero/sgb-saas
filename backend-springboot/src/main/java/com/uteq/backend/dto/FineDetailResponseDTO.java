package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO enriquecido para el listado de multas con información del libro,
 * fechas del préstamo y saldos de pago parcial.
 * Endpoint: GET /api/v1/multas/usuario/{id}/detalle
 *
 * @param id identificador de la multa
 * @param loanId identificador del préstamo que originó la multa
 * @param bookTitle título del libro prestado
 * @param bookIsbn ISBN del libro prestado
 * @param observations observaciones de la multa
 * @param amount monto total de la multa
 * @param amountPaid monto ya pagado
 * @param balance saldo pendiente
 * @param statusFineId identificador del estado de la multa
 * @param statusName nombre del estado de la multa
 * @param dateGenerated fecha de generación
 * @param datePaid fecha de pago, null si está pendiente
 * @param dateLoanStart fecha de inicio del préstamo
 * @param dateLoanFin fecha estimada de devolución del préstamo
 * @param daysAtraso días de atraso acumulados
 */
public record FineDetailResponseDTO(
        @JsonProperty("id") Long id, @JsonProperty("prestamoId") Long loanId, @JsonProperty("libroTitulo") String bookTitle, @JsonProperty("libroIsbn") String bookIsbn, @JsonProperty("observaciones") String observations, @JsonProperty("monto") BigDecimal amount, @JsonProperty("montoPagado") BigDecimal amountPaid, @JsonProperty("saldo") BigDecimal balance, @JsonProperty("estadoMultaId") Integer statusFineId, @JsonProperty("estadoNombre") String statusName, @JsonProperty("fechaGenerada") OffsetDateTime dateGenerated, @JsonProperty("fechaPagada") OffsetDateTime datePaid, @JsonProperty("fechaPrestamoInicio") OffsetDateTime dateLoanStart, @JsonProperty("fechaPrestamoFin") OffsetDateTime dateLoanFin, @JsonProperty("diasAtraso") int daysAtraso
) {}
