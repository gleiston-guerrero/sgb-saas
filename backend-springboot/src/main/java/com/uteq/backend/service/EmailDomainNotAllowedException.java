package com.uteq.backend.service;

public class EmailDomainNotAllowedException extends RuntimeException {
    /**
     * Constructor con el dominio rechazado por la lista de permitidos.
     *
     * @param message detalle del dominio no permitido
     */
    public EmailDomainNotAllowedException(String message) {
        super(message);
    }
}
