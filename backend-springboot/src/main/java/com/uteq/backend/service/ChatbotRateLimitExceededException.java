package com.uteq.backend.service;

/**
 * Límite de mensajes al chatbot excedido → HTTP 429.
 */
public class ChatbotRateLimitExceededException extends RuntimeException {

    /**
     * Constructor con el mensaje que explica cuándo puede reintentar el lector.
     *
     * @param message detalle del límite de mensajes alcanzado
     */
    public ChatbotRateLimitExceededException(String message) {
        super(message);
    }
}
