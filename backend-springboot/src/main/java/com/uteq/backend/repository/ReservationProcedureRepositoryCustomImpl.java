package com.uteq.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
     * {@code OffsetDateTime.now()} como IN: un CALL emitido desde SQL
     * plano (no PL/pgSQL) no puede depender del DEFAULT de la función
     * envuelta. Binding exclusivamente posicional -- ver nota extensa en
     * {@link LoanProcedureRepositoryCustomImpl#spCreateLoanProcedure}.
     *
     * @return cantidad de reservaciones expiradas en esta ejecución
     */
    @Override
    public Integer spExpireReservationsVencidasProcedure() {
        Query q = em.createNativeQuery("CALL proc_expirar_reservaciones_vencidas(?1, NULL)");
        q.setParameter(1, OffsetDateTime.now());
        Object result = q.getSingleResult();
        Object value = (result instanceof Object[] row) ? row[0] : result;
        return ((Number) value).intValue();
    }
}
