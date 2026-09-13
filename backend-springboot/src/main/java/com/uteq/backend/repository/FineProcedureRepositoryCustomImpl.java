package com.uteq.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
class FineProcedureRepositoryCustomImpl implements FineProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Pays a fine through the stored procedure sp_pagar_multa, which settles
     * the balance and decides whether the lector is unblocked.
     *
     * @param fineId identifier of the fine to pay in full
     * @return map with o_multa_id (paid fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spPayFineProcedure(Long fineId) {
        Query q = em.createNativeQuery("SELECT * FROM sp_pagar_multa(?1)");
        q.setParameter(1, fineId);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", ((Number) row[0]).longValue());
        result.put("o_usuario_desbloqueado", (Boolean) row[1]);
        return result;
    }

    /**
     * Voids a fine through the stored procedure sp_anular_multa, recording the
     * reason and the role that authorized the void for audit purposes.
     *
     * @param fineId identifier of the fine to void
     * @param reason business reason for the void, persisted in the audit trail
     * @param roleExecutor role of the staff member authorizing the void
     * @return map with o_multa_id (voided fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor) {
        Query q = em.createNativeQuery("SELECT * FROM sp_anular_multa(?1, ?2, ?3)");
        q.setParameter(1, fineId);
        q.setParameter(2, reason);
        q.setParameter(3, roleExecutor);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", ((Number) row[0]).longValue());
        result.put("o_usuario_desbloqueado", (Boolean) row[1]);
        return result;
    }
}
