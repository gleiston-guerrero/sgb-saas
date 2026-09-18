package com.uteq.backend.service;

public class StatusReservationInitialNotConfiguredException extends RuntimeException {
    /**
     * Constructor con la fila PENDIENTE faltante en el catálogo de estados de reservación.
     *
     * @param message detalle de la fila de catálogo ausente
     */
    public StatusReservationInitialNotConfiguredException(String message) {
        super(message);
    }
}