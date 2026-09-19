package com.uteq.backend.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface RecentPaymentProjection {
    /**
     * Identificador de la multa pagada.
     *
     * @return identificador de la multa.
     */
    Long getFineId();
    /**
     * Monto abonado de la multa.
     *
     * @return importe pagado.
     */
    BigDecimal getAmountPaid();
    /**
     * Instante del pago.
     *
     * @return fecha y hora del abono.
     */
    Instant getDatePaid();
    /**
     * Correo del lector que pagó.
     *
     * @return correo asociado al pago.
     */
    String getUserEmail();
    /**
     * Nombre completo del lector que pagó.
     *
     * @return nombre del pagador.
     */
    String getUserName();
    /**
     * Título del libro del préstamo multado.
     *
     * @return título del libro asociado a la multa.
     */
    String getBookTitle();
}
