package com.uteq.backend.service;

public class LimitRenewalsExceededException extends RuntimeException {

    /**
     * Constructor con el máximo de renovaciones que el préstamo ya agotó.
     *
     * @param message detalle del límite de renovaciones alcanzado
     */
    public LimitRenewalsExceededException(String message) {
        super(message);
    }
}
