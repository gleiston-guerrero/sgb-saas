package com.uteq.backend.repository.projection;

import java.time.Instant;

/**
 * Proyección de una fila retornada por la función SQL
 * {@code fn_reporte_uso_por_periodo} (db/procs/). Los nombres de los
 * getters (relajados a snake_case) deben coincidir con las columnas
 * declaradas en el {@code RETURNS TABLE} de esa función.
 */
public interface ReportUsageByPeriodProjection {

    /**
     * Inicio del período (día/semana/mes según granularidad).
     *
     * @return instante que identifica el período agregado.
     */
    Instant getPeriod();

    /**
     * Total de préstamos iniciados en el período.
     *
     * @return cantidad de préstamos registrados.
     */
    Long getTotalLoans();

    /**
     * Total de devoluciones registradas en el período.
     *
     * @return cantidad de devoluciones registradas.
     */
    Long getTotalLoanReturns();
}
