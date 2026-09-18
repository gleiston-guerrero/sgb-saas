package com.uteq.backend.service;

public class LoanOverdueException extends RuntimeException {

    /**
     * Constructor con el préstamo vencido que ya no admite renovación.
     *
     * @param message detalle del vencimiento que impide renovar
     */
    public LoanOverdueException(String message) {
        super(message);
    }
}
