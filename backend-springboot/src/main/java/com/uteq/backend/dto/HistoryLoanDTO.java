package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fila del historial reciente de préstamos de un usuario en la ventanilla
 * (GET /api/v1/prestamos/gestion/historial). El frontend lo pinta como
 * línea de tiempo: ícono según estadoNombre + multaPendiente.
 *
 * A diferencia de PrestamoResponseDTO (sin título de libro), acá el título
 * viaja resuelto y se agrega la multa pendiente asociada al préstamo (si
 * existe) para poder mostrar "Devuelto tarde (Multa pendiente)".
 *
 * @param loanId identificador del préstamo
 * @param bookId identificador del libro
 * @param bookTitle título del libro
 * @param bookIsbn ISBN del libro
 * @param authors nombres de los autores
 * @param categories nombres de las categorías
 * @param dateLoan fecha del préstamo
 * @param dateLoanReturnEstimada fecha estimada de devolución
 * @param dateLoanReturnReal fecha real de devolución, null si sigue vigente
 * @param statusName nombre del estado del préstamo
 * @param finePending si tiene multa pendiente asociada
 * @param amountFinePending monto de la multa pendiente
 * @param userName nombre del usuario
 * @param userEmail correo del usuario
 */
public record HistoryLoanDTO( @JsonProperty("prestamoId") Long loanId, @JsonProperty("libroId") Long bookId, @JsonProperty("libroTitulo") String bookTitle, @JsonProperty("libroIsbn") String bookIsbn, @JsonProperty("autores") List<String> authors, @JsonProperty("categorias") List<String> categories, @JsonProperty("fechaPrestamo") OffsetDateTime dateLoan, @JsonProperty("fechaDevolucionEstimada") OffsetDateTime dateLoanReturnEstimada, @JsonProperty("fechaDevolucionReal") OffsetDateTime dateLoanReturnReal, @JsonProperty("estadoNombre") String statusName, @JsonProperty("multaPendiente") boolean finePending, @JsonProperty("montoMultaPendiente") BigDecimal amountFinePending, @JsonProperty("usuarioNombre") String userName,
        @JsonProperty("usuarioCorreo") String userEmail
) {}
