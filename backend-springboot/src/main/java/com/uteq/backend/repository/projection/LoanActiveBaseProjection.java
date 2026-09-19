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

    /**
     * Identificador del préstamo activo.
     *
     * @return identificador del préstamo.
     */
    Long getLoanId();

    /**
     * Título del libro prestado.
     *
     * @return título del libro asociado.
     */
    String getBookTitle();

    /**
     * ISBN del libro prestado.
     *
     * @return ISBN del libro asociado.
     */
    String getBookIsbn();

    /**
     * Fecha de inicio del préstamo.
     *
     * @return instante de registro del préstamo.
     */
    OffsetDateTime getDateLoan();

    /**
     * Fecha estimada de devolución.
     *
     * @return plazo calculado para devolver el libro.
     */
    OffsetDateTime getDateLoanReturnEstimada();

    /**
     * Nombre del estado del préstamo.
     *
     * @return etiqueta del estado vigente.
     */
    String getStatusName();
}
