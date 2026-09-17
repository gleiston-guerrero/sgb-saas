package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Fila del historial de devoluciones del bibliotecario.
 * Se muestra al entrar al módulo de devoluciones.
 *
 * @param loanId identificador del préstamo
 * @param bookTitle título del libro
 * @param bookIsbn ISBN del libro
 * @param userName nombre del usuario
 * @param dateLoan fecha del préstamo
 * @param dateLoanReturnEstimada fecha estimada de devolución
 * @param dateLoanReturnReal fecha real de devolución
 * @param statusLoanReturn estado de la devolución
 * @param amountTotalFines monto total de multas generadas
 * @param librarianName nombre del bibliotecario que registró
 * @param dateRegistration fecha del registro
 */
public record LoanReturnHistoryDTO( @JsonProperty("prestamoId") Long loanId, @JsonProperty("libroTitulo") String bookTitle, @JsonProperty("libroIsbn") String bookIsbn, @JsonProperty("usuarioNombre") String userName, @JsonProperty("fechaPrestamo") OffsetDateTime dateLoan, @JsonProperty("fechaDevolucionEstimada") OffsetDateTime dateLoanReturnEstimada, @JsonProperty("fechaDevolucionReal") OffsetDateTime dateLoanReturnReal, @JsonProperty("estadoDevolucion") String statusLoanReturn, @JsonProperty("montoTotalMultas") BigDecimal amountTotalFines, @JsonProperty("bibliotecarioNombre") String librarianName,
        @JsonProperty("fechaRegistro") OffsetDateTime dateRegistration
) {}
