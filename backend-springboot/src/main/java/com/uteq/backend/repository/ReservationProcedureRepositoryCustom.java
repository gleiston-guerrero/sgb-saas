package com.uteq.backend.repository;

/**
 * Fragmento custom para el procedimiento de expiracion de reservaciones.
 * Implementado en ReservationProcedureRepositoryCustomImpl con EntityManager
 * y binding posicional para evitar sintaxis nombre => ? generada por Hibernate 6.
 */
public interface ReservationProcedureRepositoryCustom {
    /**
     * Expira las reservaciones vencidas mediante {@code proc_expirar_reservaciones_vencidas}
     * con la hora actual como corte.
     *
     * @return cantidad de reservaciones expiradas en esta ejecución
     */
    Integer spExpireReservationsVencidasProcedure();
}
