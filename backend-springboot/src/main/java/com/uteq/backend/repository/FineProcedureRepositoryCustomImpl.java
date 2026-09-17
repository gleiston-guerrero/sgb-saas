package com.uteq.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Repository
class FineProcedureRepositoryCustomImpl implements FineProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Pays a fine through the stored procedure proc_pagar_multa (CREATE
     * PROCEDURE nativo de V51, invocado con CALL), que envuelve la función
     * sp_pagar_multa -- settles the balance and decides whether the lector
     * is unblocked. Binding exclusivamente posicional: ver nota extensa en
     * {@link LoanProcedureRepositoryCustomImpl#spCreateLoanProcedure} sobre
     * el conflicto Hibernate 6 / pgjdbc con parámetros nombrados. Sin SQL
     * nativo: P5.
     *
     * @param fineId identifier of the fine to pay in full
     * @return map with o_multa_id (paid fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spPayFineProcedure(Long fineId) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_pagar_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(3, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(2));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(3));
        return result;
    }

    /**
     * Voids a fine through the stored procedure proc_anular_multa (CREATE
     * PROCEDURE nativo de V51, SECURITY DEFINER, invocado con CALL), que
     * envuelve sp_anular_multa -- recording the reason and the role that
     * authorized the void for audit purposes.
     *
     * @param fineId identifier of the fine to void
     * @param reason business reason for the void, persisted in the audit trail
     * @param roleExecutor role of the staff member authorizing the void
     * @return map with o_multa_id (voided fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_anular_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(4, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(5, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.setParameter(2, reason);
        sp.setParameter(3, roleExecutor);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(4));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(5));
        return result;
    }

    /**
     * Registra un pago parcial a través de proc_pago_parcial_multa
     * (wrapper V54 de sp_pago_parcial_multa, FUNCTION con 4 OUT).
     * Mismas claves de salida que el SELECT anterior
     * (o_multa_id/o_estado/o_saldo_restante/o_usuario_desbloqueado).
     *
     * @param fineId identificador de la multa a abonar
     * @param amountPaid monto del abono parcial
     * @return mapa con las 4 salidas del procedimiento
     */
    @Override
    public Map<String, Object> spPaymentParcialFine(Long fineId, BigDecimal amountPaid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_pago_parcial_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(5, BigDecimal.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(6, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.setParameter(2, amountPaid);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(3));
        result.put("o_estado", (String) sp.getOutputParameterValue(4));
        result.put("o_saldo_restante", (BigDecimal) sp.getOutputParameterValue(5));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(6));
        return result;
    }
}
