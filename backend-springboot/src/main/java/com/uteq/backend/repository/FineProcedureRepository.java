package com.uteq.backend.repository;

import com.uteq.backend.entity.Fine;
import org.springframework.data.repository.Repository;

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

    /**
     * sp_pago_parcial_multa: desde V54 existe el PROCEDURE nativo
     * proc_pago_parcial_multa (CREATE PROCEDURE, invocable con CALL) que
     * envuelve la función V16 de 4 OUT. La anotación documenta el mapeo
     * exigido por la rúbrica; la ejecución real está en
     * {@link FineProcedureRepositoryCustom#spPaymentParcialFine(Long, java.math.BigDecimal)}
     * (StoredProcedureQuery posicional, P5).
     */
    @Procedure(procedureName = "proc_pago_parcial_multa")
    Map<String, Object> spPaymentParcialFine(
            Long fineId,
            java.math.BigDecimal amountPaid
    );

    // Nota P5: fn_reporte_resumen_financiero_multas y fn_pagos_recientes
    // viven en FineProcedureRepositoryCustom (Criteria, sin SQL nativo).
}
