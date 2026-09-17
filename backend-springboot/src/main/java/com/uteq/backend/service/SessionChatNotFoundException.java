package com.uteq.backend.service;

/**
 * Sesión de chat inexistente o de otro usuario → HTTP 404.
 */
public class SessionChatNotFoundException extends RuntimeException {

    /**
     * Constructor con la sesión inexistente o ajena que se reporta como no encontrada.
     *
     * @param message detalle de la sesión de chat no encontrada
     */
    public SessionChatNotFoundException(String message) {
        super(message);
    }
}
