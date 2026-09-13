package com.uteq.backend.repository;

import com.uteq.backend.entity.Reservation;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;

/**
 * Invocación de rutinas de db/procs/ relacionadas con reservaciones.
 * Repositorio "solo rutinas" (no extiende JpaRepository).
 */
@org.springframework.stereotype.Repository
public interface ReservationProcedureRepository extends Repository<Reservation, Long>, ReservationProcedureRepositoryCustom {

    /**
     * Desde V51 existe el PROCEDURE nativo proc_expirar_reservaciones_vencidas
     * (CREATE PROCEDURE, invocable con CALL) que envuelve la función
     * sp_expirar_reservaciones_vencidas. La anotación documenta el mapeo
     * exigido por la rúbrica; la ejecución real está en
     * {@link ReservationProcedureRepositoryCustom#spExpireReservationsVencidasProcedure()}
     * por el mismo motivo que
     * {@link LoanProcedureRepository#spCreateLoanProcedure}.
     */
    @Procedure(procedureName = "proc_expirar_reservaciones_vencidas")
    Integer spExpireReservationsVencidasProcedure();
}
