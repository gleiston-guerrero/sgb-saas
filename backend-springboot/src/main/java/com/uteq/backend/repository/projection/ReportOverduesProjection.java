package com.uteq.backend.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface ReportOverduesProjection {

    /**
     * Identificador del préstamo vencido.
     *
     * @return identificador del préstamo.
     */
    Long getLoanId();

    /**
     * Nombre completo del lector.
     *
     * @return nombre visible del lector.
     */
    String getUserName();

    /**
     * Correo del lector.
     *
     * @return correo institucional o registrado del lector.
     */
    String getUserEmail();

    /**
     * Título del libro prestado.
     *
     * @return título del ejemplar prestado.
     */
    String getBookTitle();

    /**
     * ISBN del libro prestado.
     *
     * @return ISBN del ejemplar prestado.
     */
    String getBookIsbn();

    /**
     * Instante de la devolución estimada incumplida.
     *
     * @return fecha límite del préstamo vencido.
     */
    Instant getDateLoanReturnEstimada();

    /**
     * Días de atraso a la fecha.
     *
     * @return número de días vencidos.
     */
    Long getDaysAtraso();

    /**
     * Multa estimada (días por tarifa diaria).
     *
     * @return importe estimado de la multa.
     */
    BigDecimal getAmountFineEstimada();
}
