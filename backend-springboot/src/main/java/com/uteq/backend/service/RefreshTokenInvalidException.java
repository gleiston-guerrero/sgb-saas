package com.uteq.backend.service;

public class RefreshTokenInvalidException extends RuntimeException {

    /**
     * Constructor con el motivo por el que el token de refresco no permitió renovar la sesión.
     *
     * @param message detalle del refresco inválido o expirado
     */
    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
