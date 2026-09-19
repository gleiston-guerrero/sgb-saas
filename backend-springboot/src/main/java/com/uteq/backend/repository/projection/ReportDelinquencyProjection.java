package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

/**
 * Proyección de una fila retornada por la función SQL
 * {@code fn_reporte_indice_morosidad} (db/procs/). Los nombres de los
 * getters (relajados a snake_case) deben coincidir con las columnas
 * declaradas en el {@code RETURNS TABLE} de esa función.
 */
public interface ReportDelinquencyProjection {

    /**
     * Identificador del lector moroso.
     *
     * @return identificador del lector.
     */
    Long getUserId();

    /**
     * Nombre del lector.
     *
     * @return nombre del lector.
     */
    String getName();

    /**
     * Apellido del lector.
     *
     * @return apellido del lector.
     */
    String getLastName();

    /**
     * Correo del lector.
     *
     * @return correo registrado del lector.
     */
    String getEmail();

    /**
     * Deuda total pendiente del lector.
     *
     * @return importe total aún adeudado.
     */
    BigDecimal getAmountTotalAdeudado();

    /**
     * Cantidad de multas pendientes del lector.
     *
     * @return número de multas sin pagar.
     */
    Long getQuantityFinesPendientes();

    /**
     * Promedio de días de atraso del lector con 1 decimal.
     *
     * @return promedio de días de mora.
     */
    BigDecimal getDaysAtrasoPromedio();
}
