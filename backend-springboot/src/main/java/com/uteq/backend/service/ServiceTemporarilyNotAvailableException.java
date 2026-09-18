package com.uteq.backend.service;

/**
 * Dependencia externa no disponible (ej. Redis caído) → HTTP 503 para reintentar más tarde.
 */
public class ServiceTemporarilyNotAvailableException extends RuntimeException {

    /**
     * Constructor con la dependencia externa que obligó a responder 503.
     *
     * @param message detalle de la dependencia no disponible
     */
    public ServiceTemporarilyNotAvailableException(String message) {
        super(message);
    }
}
