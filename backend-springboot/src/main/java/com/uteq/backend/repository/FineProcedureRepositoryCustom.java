package com.uteq.backend.repository;

import com.uteq.backend.repository.projection.RecentPaymentProjection;
import com.uteq.backend.repository.projection.SummaryFinancialFinesProjection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fragmento custom para stored procedures de multas (P5: StoredProcedureQuery
 * posicional) y reportes tabulares reimplementados sin SQL nativo.
 */
public interface FineProcedureRepositoryCustom {
    /**
     * Paga la multa dada mediante {@code proc_pagar_multa}; desbloquea al lector si salda su deuda.
     *
     * @param fineId identificador de la multa a pagar
     * @return mapa con {@code o_multa_id} y {@code o_usuario_desbloqueado}
     */
    Map<String, Object> spPayFineProcedure(Long fineId);
    /**
     * Anula la multa dada mediante {@code proc_anular_multa} con motivo y rol autorizante.
     *
     * @param fineId identificador de la multa a anular
     * @param reason motivo de la anulación
     * @param roleExecutor rol del personal que autoriza la anulación
     * @return mapa con {@code o_multa_id} y {@code o_usuario_desbloqueado}
     */
    Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor);
    /**
     * Registra un abono parcial mediante {@code proc_pago_parcial_multa}.
     *
     * @param fineId identificador de la multa a abonar
     * @param amountPaid monto del abono parcial
     * @return mapa con {@code o_multa_id}, {@code o_estado}, {@code o_saldo_restante}
     *         y {@code o_usuario_desbloqueado}
     */
    Map<String, Object> spPaymentParcialFine(Long fineId, BigDecimal amountPaid);
    /**
     * Réplica de {@code fn_reporte_resumen_financiero_multas}: total recaudado (PAGADA)
     * y pendiente (PENDIENTE) en el rango.
     *
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return fila única con ambos totales (ceros si no hay multas)
     */
    SummaryFinancialFinesProjection fnReportSummaryFinancial(OffsetDateTime from, OffsetDateTime until);
    /**
     * Réplica de {@code fn_pagos_recientes}: últimas multas PAGADAs con lector y libro,
     * por fecha de pago descendente.
     *
     * @param limit tope de filas, nulo = 5
     * @return lista de pagos recientes
     */
    List<RecentPaymentProjection> fnPaymentsRecientes(Integer limit);
}
