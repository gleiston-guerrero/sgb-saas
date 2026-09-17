package com.uteq.backend.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface RecentPaymentProjection {
    /** Identificador de la multa pagada. */
    Long getFineId();
    /** Monto abonado de la multa. */
    BigDecimal getAmountPaid();
    /** Instante del pago. */
    Instant getDatePaid();
    /** Correo del lector que pagó. */
    String getUserEmail();
    /** Nombre completo del lector que pagó. */
    String getUserName();
    /** Título del libro del préstamo multado. */
    String getBookTitle();
}
