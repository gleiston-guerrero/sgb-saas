package com.uteq.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;


// DTO de respuesta HTTP para fn_reporte_libros_mas_prestados -- mismo
// criterio que PrestamoActivoResponseDTO: envuelve la proyección en vez
// de exponerla directamente en la respuesta.
/**
 * Fila del reporte de libros más prestados.
 *
 * @param bookId identificador del libro
 * @param title título del libro
 * @param isbn ISBN del libro
 * @param totalLoans total de préstamos del libro
 */
public record BookMostLoanedResponseDTO( @JsonProperty("libroId") Long bookId, @JsonProperty("titulo") String title,
        String isbn, @JsonProperty("totalPrestamos") Long totalLoans
) {}
