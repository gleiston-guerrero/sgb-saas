package com.uteq.backend.repository;

import com.uteq.backend.entity.Loan;
import com.uteq.backend.repository.projection.LoanActiveBaseProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * CRUD elemental sobre {@code prestamos}. Solo consultas derivadas de una
 * sola tabla (sin joins) — cualquier lectura que combine préstamos con
 * libros/estados vive en {@link LoanProcedureRepository}
 * (fn_listar_prestamos_activos_por_usuario, fn_reporte_libros_mas_prestados).
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByUserId(Long userId, Pageable pageable);

    Page<Loan> findByStatusLoanId(Integer statusId, Pageable pageable);

    // Usada por NotificacionVencimientoScheduler: préstamos vigentes
    // (ACTIVO/RENOVADO -- estadoIds ya resueltos por el llamador, ver
    // EstadoPrestamoRepository) cuya fecha_devolucion_estimada cae dentro
    // de la ventana [ahora, ahora + minutos de anticipación configurados].
    List<Loan> findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
            List<Integer> statusLoanIds, OffsetDateTime from, OffsetDateTime until);

    // Historial reciente del usuario, más nuevo primero (línea de tiempo acotada, sin paginación).
    List<Loan> findByUserIdOrderByIdDesc(Long userId);

    // Préstamos activos del LECTOR: misma lógica que
    // fn_listar_prestamos_activos_por_usuario pero en JPQL para no
    // depender del stored procedure en producción (P5).
    // JOINs cartesianos porque Loan expone FK planas sin relaciones JPA.
    // Los días restantes se calculan en Java (ver LoanService y
    // QueryLoansTool) con la semántica de la fórmula original
    // (fecha_devolucion_estimada::date - NOW()::date).
    @Query("""
        SELECT p.id AS loanId, b.title AS bookTitle, b.isbn AS bookIsbn,
               p.dateLoan AS dateLoan,
               p.dateLoanReturnEstimada AS dateLoanReturnEstimada,
               s.name AS statusName
        FROM Loan p, Book b, StatusLoan s
        WHERE b.id = p.bookId
          AND s.id = p.statusLoanId
          AND p.userId = :userId
          AND s.name <> 'DEVUELTO'
        ORDER BY p.dateLoanReturnEstimada ASC""")
    List<LoanActiveBaseProjection> findActivesByUserId(@Param("userId") Long userId);
}
