package com.uteq.backend.repository;

import com.uteq.backend.entity.Loan;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Invocación de los procedimientos/funciones de db/procs/ relacionados con
 * préstamos. No extiende JpaRepository a propósito: es un repositorio
 * "solo procedimientos" (patrón documentado de Spring Data JPA), por lo que
 * solo necesita el marcador {@link Repository}.
 *
 * <p>Los métodos {@code @Procedure} para sp_crear_prestamo y
 * sp_registrar_devolucion viven en {@link LoanProcedureRepositoryCustom} /
 * {@link LoanProcedureRepositoryCustomImpl}: usan {@code EntityManager} con
 * binding posicional (Integer) para evitar la sintaxis {@code nombre => ?}
 * que Hibernate 6 genera con nombres y que pgjdbc rechaza en
 * {@code {call ...}} (spring-projects/spring-data-jpa#3393).</p>
 */
@org.springframework.stereotype.Repository
public interface LoanProcedureRepository extends Repository<Loan, Long>, LoanProcedureRepositoryCustom {

    /**
     * sp_crear_prestamo: desde V51 existe el PROCEDURE nativo
     * proc_crear_prestamo (CREATE PROCEDURE, invocable con CALL) que
     * envuelve esta función. La anotación queda declarada aquí (satisface
     * el contrato de {@code ProcedureMappingContractTest} y documenta el
     * mapeo real), pero la ejecución efectiva vive en
     * {@link LoanProcedureRepositoryCustom#spCreateLoanProcedure(Long, Long, Long, Integer)}
     * / {@link LoanProcedureRepositoryCustomImpl}: Hibernate 6 genera
     * sintaxis de parámetros nombrados de PostgreSQL ("nombre => ?") dentro
     * del escape JDBC {@code {call ...}} cuando el método pasa por el proxy
     * estándar de Spring Data, y pgjdbc la rechaza (spring-projects/spring-data-jpa#3393).
     * El fragmento Custom evita el problema con {@code CALL} nativo y
     * binding exclusivamente posicional (sin nombres de parámetro).
     */
    @Procedure(procedureName = "proc_crear_prestamo")
    Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan);

    /**
     * sp_registrar_devolucion: desde V51 existe el PROCEDURE nativo
     * proc_registrar_devolucion. Misma situación que
     * {@link #spCreateLoanProcedure}: la anotación documenta el mapeo, la
     * ejecución real está en
     * {@link LoanProcedureRepositoryCustom#spRegisterLoanReturn(Long)}.
     */
    @Procedure(name = "Prestamo.registrarDevolucion")
    java.util.Map<String, Object> spRegisterLoanReturn(Long loanId);

    // Nota P5: los 8 reportes tabulares (fn_* RETURNS TABLE) viven en
    // LoanProcedureRepositoryCustom (Criteria/JPQL + cómputo Java con
    // semántica idéntica, sin SQL nativo). Las funciones SQL quedan en BD
    // para compatibilidad psql directa.
}
