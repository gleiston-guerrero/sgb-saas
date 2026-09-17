package com.uteq.backend.repository.projection;

import java.time.OffsetDateTime;

/**
 * Proyección base de préstamos activos por usuario (sin días restantes).
 *
 * <p>Productores: {@code LoanRepository.findActivesByUserId} y
 * {@code LoanProcedureRepositoryCustom.fnListLoansActivesByUser}, ambos en
 * JPQL (P5). Los días restantes se calculan en Java con la misma semántica
 * que la fórmula nativa original
 * {@code (fecha_devolucion_estimada::date - NOW()::date)}.
 */
public interface LoanActiveBaseProjection {

    Long getLoanId();

    String getBookTitle();

    String getBookIsbn();

    OffsetDateTime getDateLoan();

    OffsetDateTime getDateLoanReturnEstimada();

    String getStatusName();
}
