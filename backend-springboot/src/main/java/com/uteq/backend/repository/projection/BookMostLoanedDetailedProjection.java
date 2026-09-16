package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

/**
 * Fila del reporte de libros más prestados con autor, categoría y porcentaje.
 */
public interface BookMostLoanedDetailedProjection {

    /** Identificador del libro. */
    Long getBookId();

    /** Título del libro. */
    String getTitle();

    /** ISBN del libro. */
    String getIsbn();

    /** Nombres de autores concatenados. */
    String getAuthorName();

    /** Nombres de categorías concatenadas. */
    String getCategoryName();

    /** Total de préstamos del libro. */
    Long getTotalLoans();

    /** Porcentaje sobre el total de préstamos. */
    BigDecimal getPercentage();
}
