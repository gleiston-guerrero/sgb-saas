package com.uteq.backend.repository;

import com.uteq.backend.entity.Fine;
import com.uteq.backend.repository.projection.RecentPaymentProjection;
import com.uteq.backend.repository.projection.SummaryFinancialFinesProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.data.jpa.repository.query.Procedure;

@org.springframework.stereotype.Repository
public interface FineProcedureRepository extends Repository<Fine, Long>, FineProcedureRepositoryCustom {

    /**
     * Desde V51 existe el PROCEDURE nativo proc_pagar_multa (CREATE
     * PROCEDURE, invocable con CALL). La anotación documenta el mapeo
     * exigido por la rúbrica (ver {@code ProcedureMappingContractTest});
     * la ejecución real está en
     * {@link FineProcedureRepositoryCustom#spPayFineProcedure(Long)} por el
     * mismo motivo que {@link com.uteq.backend.repository.LoanProcedureRepository#spCreateLoanProcedure}.
     */
    @Procedure(name = "Multa.pagarMulta")
    Map<String, Object> spPayFineProcedure(Long fineId);

    /**
     * Desde V51 existe el PROCEDURE nativo proc_anular_multa (SECURITY
     * DEFINER, invocable con CALL). Misma situación que
     * {@link #spPayFineProcedure}.
     */
    @Procedure(name = "Multa.anularMulta")
    Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor);

    // sp_pago_parcial_multa: funcion con efectos secundarios y 4 parametros
    // OUT (V16). Se evaluo para P4 junto con las 5 de V51, pero no es "SQL
    // plano por comodidad": es una invocacion a una rutina almacenada igual
    // que las demas de este archivo, sin equivalente JPQL posible (JPQL no
    // invoca funciones definidas por el usuario con parametros OUT). Queda
    // fuera del alcance de la conversion nativeQuery->JPQL; envolverla en un
    // PROCEDURE nuevo (mismo patron que V51) es una extension valida a
    // futuro, no un cambio de bajo riesgo para esta sesion.
    @Query(value = "SELECT * FROM sp_pago_parcial_multa(:p_multa_id, :p_monto_pagado)", nativeQuery = true)
    Map<String, Object> spPaymentParcialFine(
            @Param("p_multa_id") Long fineId,
            @Param("p_monto_pagado") java.math.BigDecimal amountPaid
    );

    @Query(value = "SELECT total_recaudado AS totalRecaudado, total_pendiente AS totalPending "
            + "FROM fn_reporte_resumen_financiero_multas(:p_desde, :p_hasta)", nativeQuery = true)
    SummaryFinancialFinesProjection fnReportSummaryFinancial(
            @Param("p_desde") OffsetDateTime from,
            @Param("p_hasta") OffsetDateTime until
    );

    @Query(value = "SELECT multa_id AS fineId, monto_pagado AS amountPaid, fecha_pagada AS datePaid, "
            + "usuario_correo AS userEmail, usuario_nombre AS userName, libro_titulo AS bookTitle "
            + "FROM fn_pagos_recientes(:p_limit)", nativeQuery = true)
    java.util.List<RecentPaymentProjection> fnPaymentsRecientes(
            @Param("p_limit") Integer limit
    );
}
