package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

/**
 * Fila del reporte de libros más prestados con autor, categoría y porcentaje.
 */
public interface BookMostLoanedDetailedProjection {

    /**
     * Identificador del libro.
     *
     * @return identificador persistente del libro.
     */
    Long getBookId();

    /**
     * Título del libro.
     *
     * @return título mostrado en el reporte.
     */
    String getTitle();

    /**
     * ISBN del libro.
     *
     * @return ISBN del libro.
     */
    String getIsbn();

    /**
     * Nombres de autores concatenados.
     *
     * @return autores asociados al libro.
     */
    String getAuthorName();

    /**
     * Nombres de categorías concatenadas.
     *
     * @return categorías asociadas al libro.
     */
    String getCategoryName();

    /**
     * Total de préstamos del libro.
     *
     * @return cantidad acumulada de préstamos.
     */
    Long getTotalLoans();

    /**
     * Porcentaje sobre el total de préstamos.
     *
     * @return porcentaje de participación del libro.
     */
    BigDecimal getPercentage();
}
