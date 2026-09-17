package com.uteq.backend.service;

public class LimitLoansExceededException extends RuntimeException {
    /**
     * Constructor con el tope de préstamos activos que el usuario ya alcanzó.
     *
     * @param message detalle de los préstamos activos frente al máximo permitido
     */
    public LimitLoansExceededException(String message) {
        super(message);
    }
}
