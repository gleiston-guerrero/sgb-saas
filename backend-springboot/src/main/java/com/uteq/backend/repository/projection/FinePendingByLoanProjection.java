package com.uteq.backend.repository.projection;

import java.math.BigDecimal;

/**
 * Proyección de una fila del agregado de multas PENDIENTES agrupadas por
 * préstamo (MultaRepository.findPendientesAgrupadasPorPrestamo). Se usa en
 * el historial de la ventanilla de préstamos para marcar qué préstamos
 * devueltos tardíamente todavía arrastran una multa sin pagar, y por cuánto.
 */
public interface FinePendingByLoanProjection {

    /**
     * Identificador del préstamo con multa pendiente.
     *
     * @return identificador del préstamo.
     */
    Long getLoanId();

    /**
     * Suma de montos pendientes del préstamo.
     *
     * @return importe acumulado aún pendiente.
     */
    BigDecimal getTotalPending();
}
