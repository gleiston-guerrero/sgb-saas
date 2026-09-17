package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Fila del reporte detallado de libros más prestados.
 *
 * @param bookId identificador del libro
 * @param title título del libro
 * @param isbn ISBN del libro
 * @param authorName nombres de autores concatenados
 * @param categoryName nombres de categorías concatenadas
 * @param totalLoans total de préstamos del libro
 * @param percentage porcentaje sobre el total de préstamos
 */
public record BookMostLoanedDetailedResponseDTO( @JsonProperty("libroId") Long bookId, @JsonProperty("titulo") String title,
        String isbn, @JsonProperty("autorNombre") String authorName, @JsonProperty("categoriaNombre") String categoryName, @JsonProperty("totalPrestamos") Long totalLoans, @JsonProperty("porcentaje") BigDecimal percentage
) {}
