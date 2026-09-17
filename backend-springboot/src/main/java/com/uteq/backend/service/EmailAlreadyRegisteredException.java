package com.uteq.backend.service;

public class EmailAlreadyRegisteredException extends RuntimeException {

    /**
     * Constructor con el correo duplicado que impidió crear la cuenta.
     *
     * @param message detalle del correo ya registrado
     */
    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
