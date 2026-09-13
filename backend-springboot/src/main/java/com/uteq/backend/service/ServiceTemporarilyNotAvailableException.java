package com.uteq.backend.service;

/**
 * Dependencia externa no disponible (ej. Redis caído) → HTTP 503 para reintentar más tarde.
 */
public class ServiceTemporarilyNotAvailableException extends RuntimeException {

    public ServiceTemporarilyNotAvailableException(String message) {
        super(message);
    }
}
