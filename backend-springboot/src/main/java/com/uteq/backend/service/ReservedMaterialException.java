package com.uteq.backend.service;

public class ReservedMaterialException extends RuntimeException {

    /**
     * Constructor con el libro que tiene reserva vigente de otro usuario.
     *
     * @param message detalle de la reserva vigente que impide renovar
     */
    public ReservedMaterialException(String message) {
        super(message);
    }
}
