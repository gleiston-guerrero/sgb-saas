package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

public interface ReportCategoriesDemandedProjection {

    /**
     * Identificador de la categoría.
     *
     * @return identificador de la categoría.
     */
    Integer getCategoryId();

    /**
     * Nombre de la categoría.
     *
     * @return nombre de la categoría.
     */
    String getCategoryName();

    /**
     * Total de préstamos de libros de la categoría.
     *
     * @return cantidad acumulada de préstamos.
     */
    Long getTotalLoans();

    /**
     * Porcentaje sobre el total de préstamos.
     *
     * @return participación porcentual de la categoría.
     */
    BigDecimal getPercentage();
}
