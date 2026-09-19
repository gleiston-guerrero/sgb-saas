package com.uteq.backend.repository.projection;

/**
 * Proyección de una fila retornada por la función SQL
 * {@code fn_reporte_libros_mas_prestados} (db/procs/). Los nombres de los
 * getters (relajados a snake_case) deben coincidir con las columnas
 * declaradas en el {@code RETURNS TABLE} de esa función.
 */
public interface BookMostLoanedProjection {

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
     * @return ISBN asociado al libro.
     */
    String getIsbn();

    /**
     * Total de préstamos del libro.
     *
     * @return cantidad acumulada de préstamos.
     */
    Long getTotalLoans();
}
