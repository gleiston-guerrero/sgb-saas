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

    /** Identificador del préstamo activo. */
    Long getLoanId();

    /** Título del libro prestado. */
    String getBookTitle();

    /** ISBN del libro prestado. */
    String getBookIsbn();

    /** Fecha de inicio del préstamo. */
    OffsetDateTime getDateLoan();

    /** Fecha estimada de devolución. */
    OffsetDateTime getDateLoanReturnEstimada();

    /** Nombre del estado del préstamo. */
    String getStatusName();
}
