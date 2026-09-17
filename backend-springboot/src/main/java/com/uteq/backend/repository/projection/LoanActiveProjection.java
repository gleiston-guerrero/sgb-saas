package com.uteq.backend.repository.projection;

import java.time.Instant;

/**
 * Proyección de una fila de préstamos activos por usuario.
 * Productores: la query nativa {@code LoanRepository.findActivesByUserId}
 * (alias en inglés) y la función SQL
 * {@code fn_listar_prestamos_activos_por_usuario} (db/procs/).
 * Los nombres de los getters (relajados a snake_case) deben coincidir
 * con los alias/columnas declaradas en ambos productores.
 */
public interface LoanActiveProjection {

    Long getLoanId();

    String getBookTitle();

    String getBookIsbn();

    Instant getDateLoan();

    Instant getDateLoanReturnEstimada();

    Integer getDaysRemaining();

    String getStatusName();
}
