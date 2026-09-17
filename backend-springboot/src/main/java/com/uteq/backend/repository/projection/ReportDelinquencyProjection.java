package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

/**
 * Proyección de una fila retornada por la función SQL
 * {@code fn_reporte_indice_morosidad} (db/procs/). Los nombres de los
 * getters (relajados a snake_case) deben coincidir con las columnas
 * declaradas en el {@code RETURNS TABLE} de esa función.
 */
public interface ReportDelinquencyProjection {

    /** Identificador del lector moroso. */
    Long getUserId();

    /** Nombre del lector. */
    String getName();

    /** Apellido del lector. */
    String getLastName();

    /** Correo del lector. */
    String getEmail();

    /** Deuda total pendiente del lector. */
    BigDecimal getAmountTotalAdeudado();

    /** Cantidad de multas pendientes del lector. */
    Long getQuantityFinesPendientes();

    /** Promedio de días de atraso del lector con 1 decimal. */
    BigDecimal getDaysAtrasoPromedio();
}
