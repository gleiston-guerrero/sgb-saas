package com.uteq.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Repository
class LoanProcedureRepositoryCustomImpl implements LoanProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Creates a loan through the stored procedure proc_crear_prestamo
     * (CREATE PROCEDURE nativo de V51, invocado con CALL), que a su vez
     * envuelve la función sp_crear_prestamo -- valida stock disponible y
     * bloqueo por multas del lector.
     *
     * <p>Se invoca con {@code CALL} y binding exclusivamente posicional
     * (sin nombres de parámetro) porque el proxy estándar de
     * {@code @Procedure}/{@code @NamedStoredProcedureQuery} de Hibernate 6
     * genera sintaxis de parámetros nombrados de PostgreSQL dentro del
     * escape JDBC {@code {call ...}}, que pgjdbc rechaza
     * (spring-projects/spring-data-jpa#3393). PostgreSQL exige un
     * placeholder posicional también para cada parámetro OUT en un CALL
     * emitido desde SQL plano (no PL/pgSQL); su valor no importa, se
     * convención escribir NULL.
     *
     * @param userId identifier of the lector requesting the loan
     * @param bookId identifier of the book to loan
     * @param librarianId identifier of the librarian registering the loan
     * @param daysLoan loan term in days used to compute the estimated return date
     * @return identifier of the loan created by the procedure
     */
    @Override
    public Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan) {
        Query q = em.createNativeQuery("CALL proc_crear_prestamo(?1, ?2, ?3, ?4, NULL)");
        q.setParameter(1, userId);
        q.setParameter(2, bookId);
        q.setParameter(3, librarianId);
        q.setParameter(4, daysLoan);
        Object result = q.getSingleResult();
        Object value = (result instanceof Object[] row) ? row[0] : result;
        return ((Number) value).longValue();
    }

    /**
     * Registers a loan return through the stored procedure
     * proc_registrar_devolucion (CREATE PROCEDURE nativo de V51, invocado
     * con CALL), que envuelve sp_registrar_devolucion -- restaura stock y
     * genera multa automática si la devolución es tardía. Mismo criterio de
     * binding posicional que {@link #spCreateLoanProcedure} arriba.
     *
     * @param loanId identifier of the loan being returned
     * @return map with o_prestamo_id (returned loan id), o_hubo_multa (whether
     *         an overdue fine was generated) and o_monto_multa (fine amount, if any)
     */
    @Override
    public Map<String, Object> spRegisterLoanReturn(Long loanId) {
        Query q = em.createNativeQuery("CALL proc_registrar_devolucion(?1, NULL, NULL, NULL)");
        q.setParameter(1, loanId);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("o_prestamo_id", ((Number) row[0]).longValue());
        result.put("o_hubo_multa", (Boolean) row[1]);
        result.put("o_monto_multa", row[2] != null ? new BigDecimal(row[2].toString()) : null);
        return result;
    }
}
