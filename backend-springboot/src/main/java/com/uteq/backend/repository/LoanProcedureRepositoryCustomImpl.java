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
     * Creates a loan through the stored procedure sp_crear_prestamo, which
     * validates stock availability, lector limits and active reservations.
     *
     * @param userId identifier of the lector requesting the loan
     * @param bookId identifier of the book to loan
     * @param librarianId identifier of the librarian registering the loan
     * @param daysLoan loan term in days used to compute the estimated return date
     * @return identifier of the loan created by the procedure
     */
    @Override
    public Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan) {
        Query q = em.createNativeQuery("SELECT sp_crear_prestamo(?1, ?2, ?3, ?4)");
        q.setParameter(1, userId);
        q.setParameter(2, bookId);
        q.setParameter(3, librarianId);
        q.setParameter(4, daysLoan);
        return ((Number) q.getSingleResult()).longValue();
    }

    /**
     * Registers a loan return through the stored procedure
     * sp_registrar_devolucion, which restores stock and generates an overdue
     * fine when the return is late.
     *
     * @param loanId identifier of the loan being returned
     * @return map with o_prestamo_id (returned loan id), o_hubo_multa (whether
     *         an overdue fine was generated) and o_monto_multa (fine amount, if any)
     */
    @Override
    public Map<String, Object> spRegisterLoanReturn(Long loanId) {
        Query q = em.createNativeQuery("SELECT * FROM sp_registrar_devolucion(?1)");
        q.setParameter(1, loanId);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new HashMap<>();
        result.put("o_prestamo_id", ((Number) row[0]).longValue());
        result.put("o_hubo_multa", (Boolean) row[1]);
        result.put("o_monto_multa", row[2] != null ? new BigDecimal(row[2].toString()) : null);
        return result;
    }
}
