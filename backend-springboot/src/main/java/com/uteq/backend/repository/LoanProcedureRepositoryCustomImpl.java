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
class LoanProcedureRepositoryCustomImpl implements LoanProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Creates a loan through the stored procedure proc_crear_prestamo
     * (CREATE PROCEDURE nativo de V51, invocado con CALL), que a su vez
     * envuelve la función sp_crear_prestamo -- valida stock disponible y
     * bloqueo por multas del lector.
     *
     * <p>Se invoca con {@code StoredProcedureQuery} JPA y binding
     * exclusivamente posicional (sin nombres de parámetro) porque el proxy
     * estándar de {@code @Procedure}/{@code @NamedStoredProcedureQuery} de
     * Hibernate 6 genera sintaxis de parámetros nombrados de PostgreSQL
     * dentro del escape JDBC {@code {call ...}}, que pgjdbc rechaza
     * (spring-projects/spring-data-jpa#3393). Sin SQL nativo: P5.
     *
     * @param userId identifier of the lector requesting the loan
     * @param bookId identifier of the book to loan
     * @param librarianId identifier of the librarian registering the loan
     * @param daysLoan loan term in days used to compute the estimated return date
     * @return identifier of the loan created by the procedure
     */
    @Override
    public Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_crear_prestamo");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(4, Integer.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(5, Long.class, ParameterMode.OUT);
        sp.setParameter(1, userId);
        sp.setParameter(2, bookId);
        sp.setParameter(3, librarianId);
        sp.setParameter(4, daysLoan);
        sp.execute();
        return (Long) sp.getOutputParameterValue(5);
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
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_registrar_devolucion");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(3, Boolean.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(4, BigDecimal.class, ParameterMode.OUT);
        sp.setParameter(1, loanId);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_prestamo_id", (Long) sp.getOutputParameterValue(2));
        result.put("o_hubo_multa", (Boolean) sp.getOutputParameterValue(3));
        result.put("o_monto_multa", (BigDecimal) sp.getOutputParameterValue(4));
        return result;
    }
}
