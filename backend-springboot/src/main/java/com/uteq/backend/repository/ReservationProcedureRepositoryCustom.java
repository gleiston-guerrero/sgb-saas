package com.uteq.backend.repository;

/**
 * Fragmento custom para el procedimiento de expiracion de reservaciones.
 * Implementado en ReservationProcedureRepositoryCustomImpl con EntityManager
 * y binding posicional para evitar sintaxis nombre => ? generada por Hibernate 6.
 */
public interface ReservationProcedureRepositoryCustom {
    Integer spExpireReservationsVencidasProcedure();
}
