package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

public interface ReportCategoriesDemandedProjection {

    /** Identificador de la categoría. */
    Integer getCategoryId();

    /** Nombre de la categoría. */
    String getCategoryName();

    /** Total de préstamos de libros de la categoría. */
    Long getTotalLoans();

    /** Porcentaje sobre el total de préstamos. */
    BigDecimal getPercentage();
}
