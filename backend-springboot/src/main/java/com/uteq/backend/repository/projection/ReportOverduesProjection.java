package com.uteq.backend.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface ReportOverduesProjection {

    /** Identificador del préstamo vencido. */
    Long getLoanId();

    /** Nombre completo del lector. */
    String getUserName();

    /** Correo del lector. */
    String getUserEmail();

    /** Título del libro prestado. */
    String getBookTitle();

    /** ISBN del libro prestado. */
    String getBookIsbn();

    /** Instante de la devolución estimada incumplida. */
    Instant getDateLoanReturnEstimada();

    /** Días de atraso a la fecha. */
    Long getDaysAtraso();

    /** Multa estimada (días por tarifa diaria). */
    BigDecimal getAmountFineEstimada();
}
