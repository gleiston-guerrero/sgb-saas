package com.uteq.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
class ReservationProcedureRepositoryCustomImpl implements ReservationProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Expira en lote las reservaciones vencidas a través del stored
     * procedure proc_expirar_reservaciones_vencidas (CREATE PROCEDURE
     * nativo de V51, invocado con CALL), que envuelve la función
     * sp_expirar_reservaciones_vencidas. Se pasa explícitamente
     * {@code OffsetDateTime.now()} como IN. Binding exclusivamente
     * posicional -- ver nota extensa en
     * {@link LoanProcedureRepositoryCustomImpl#spCreateLoanProcedure}.
     * Sin SQL nativo: P5.
     *
     * @return cantidad de reservaciones expiradas en esta ejecución
     */
    @Override
    public Integer spExpireReservationsVencidasProcedure() {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_expirar_reservaciones_vencidas");
        sp.registerStoredProcedureParameter(1, OffsetDateTime.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
        sp.setParameter(1, OffsetDateTime.now());
        sp.execute();
        return (Integer) sp.getOutputParameterValue(2);
    }
}
