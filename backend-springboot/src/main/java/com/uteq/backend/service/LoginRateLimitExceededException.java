package com.uteq.backend.service;

public class LoginRateLimitExceededException extends RuntimeException {

    /**
     * Constructor con los segundos restantes antes de poder reintentar el acceso.
     *
     * @param message detalle del bloqueo por intentos fallidos
     */
    public LoginRateLimitExceededException(String message) {
        super(message);
    }
}
