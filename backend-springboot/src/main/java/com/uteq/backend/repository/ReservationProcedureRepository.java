package com.uteq.backend.repository;

import com.uteq.backend.entity.Reservation;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;

/**
 * Invocación del procedimiento de db/procs/ relacionado con reservaciones.
 * Repositorio "solo procedimientos" (no extiende JpaRepository).
 */
@org.springframework.stereotype.Repository
public interface ReservationProcedureRepository extends Repository<Reservation, Long> {

    @Procedure(procedureName = "sp_expirar_reservaciones_vencidas")
    Integer spExpireReservationsVencidasProcedure();
}
