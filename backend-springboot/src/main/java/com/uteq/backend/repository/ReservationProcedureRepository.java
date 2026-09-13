package com.uteq.backend.repository;

import com.uteq.backend.entity.Reservation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Invocación de rutinas de db/procs/ relacionadas con reservaciones.
 * Repositorio "solo rutinas" (no extiende JpaRepository).
 */
@org.springframework.stereotype.Repository
public interface ReservationProcedureRepository extends Repository<Reservation, Long> {

    @Query(value = "SELECT sp_expirar_reservaciones_vencidas()", nativeQuery = true)
    Integer spExpireReservationsVencidasProcedure();
}
