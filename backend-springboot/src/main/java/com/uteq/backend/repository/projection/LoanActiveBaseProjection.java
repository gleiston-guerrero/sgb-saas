package com.uteq.backend.repository.projection;

import java.time.OffsetDateTime;

/**
 * Proyección base de préstamos activos por usuario (sin días restantes).
 *
 * <p>Productor: {@code LoanRepository.findActivesByUserId} en JPQL (P5).
 * Los días restantes se calculan en Java con la misma semántica que la
 * fórmula nativa original
 * {@code (fecha_devolucion_estimada::date - NOW()::date)}.
 * La proyección completa {@link LoanActiveProjection} (con días) sigue
 * vigente para la función SQL {@code fn_listar_prestamos_activos_por_usuario}.
 */
public interface LoanActiveBaseProjection {

    Long getLoanId();

    String getBookTitle();

    String getBookIsbn();

    OffsetDateTime getDateLoan();

    OffsetDateTime getDateLoanReturnEstimada();

    String getStatusName();
}
