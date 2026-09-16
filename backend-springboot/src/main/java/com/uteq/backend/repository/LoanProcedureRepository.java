package com.uteq.backend.repository;

import com.uteq.backend.entity.Loan;
import com.uteq.backend.repository.projection.BookMostLoanedDetailedProjection;
import com.uteq.backend.repository.projection.BookMostLoanedProjection;
import com.uteq.backend.repository.projection.LoanActiveProjection;
import com.uteq.backend.repository.projection.ReportCategoriesDemandedProjection;
import com.uteq.backend.repository.projection.ReportInventoryProjection;
import com.uteq.backend.repository.projection.ReportDelinquencyProjection;
import com.uteq.backend.repository.projection.ReportUsageByPeriodProjection;
import com.uteq.backend.repository.projection.ReportOverduesProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    /**
     * fn_listar_prestamos_activos_por_usuario: función PostgreSQL con
     * RETURNS TABLE (varias filas). NO se pudo mapear con {@code @Procedure} ni
     * con {@code @NamedStoredProcedureQuery}: la API de stored procedures de JPA
     * 2.1 está construida sobre JDBC CallableStatement, que en PostgreSQL
     * solo expone resultados vía un valor escalar/OUT o un parámetro
     * REF_CURSOR — no vía RETURNS TABLE/SETOF invocado como función. Para
     * usar REF_CURSOR habría que reescribir la función para que abra y
     * retorne un cursor, lo cual le impediría seguir siendo invocable
     * directamente como "SELECT * FROM fn_...(...)" desde psql/otras
     * herramientas. Se usa en su lugar una @Query nativa (patrón estándar
     * de Spring Data para funciones PostgreSQL que retornan tabla).
     */
    @Query(value = "SELECT prestamo_id AS loanId, libro_titulo AS bookTitle, libro_isbn AS bookIsbn, "
            + "fecha_prestamo AS dateLoan, fecha_devolucion_estimada AS dateLoanReturnEstimada, "
            + "dias_restantes AS daysRemaining, estado_nombre AS statusName "
            + "FROM fn_listar_prestamos_activos_por_usuario(:p_user_id)", nativeQuery = true)
    List<LoanActiveProjection> fnListLoansActivesByUser(@Param("p_user_id") Long userId);

    /**
     * fn_reporte_libros_mas_prestados: misma situación que
     * fn_listar_prestamos_activos_por_usuario (RETURNS TABLE de varias
     * filas) — @Query nativa por la misma razón documentada arriba.
     */
    @Query(value = "SELECT * FROM fn_reporte_libros_mas_prestados(:p_limit, :p_from, :p_until)", nativeQuery = true)
    List<BookMostLoanedProjection> fnReportBooksMostLoaned(
            @Param("p_limit") Integer maxLimit,
            @Param("p_from") OffsetDateTime from,
            @Param("p_until") OffsetDateTime until
    );

    // Reporte de índice de morosidad (función RETURNS TABLE vía @Query nativa).
    @Query(value = "SELECT * FROM fn_reporte_indice_morosidad(:p_limit)", nativeQuery = true)
    List<ReportDelinquencyProjection> fnReportIndexDelinquency(@Param("p_limit") Integer maxLimit);

    // Reporte de uso por periodo (función RETURNS TABLE vía @Query nativa).
    @Query(value = "SELECT * FROM fn_reporte_uso_por_periodo(:p_granularidad, :p_from, :p_until)",
            nativeQuery = true)
    List<ReportUsageByPeriodProjection> fnReportUsageByPeriod(
            @Param("p_granularidad") String granularidad,
            @Param("p_from") OffsetDateTime from,
            @Param("p_until") OffsetDateTime until
    );

    /**
     * fn_reporte_libros_mas_prestados_detallado: versión extendida con
     * autor, categoría y porcentaje del total.
     */
    @Query(value = "SELECT * FROM fn_reporte_libros_mas_prestados_detallado(:p_limit, :p_from, :p_until, :p_category_id)",
            nativeQuery = true)
    List<BookMostLoanedDetailedProjection> fnReportBooksMostLoanedDetailed(
            @Param("p_limit") Integer maxLimit,
            @Param("p_from") OffsetDateTime from,
            @Param("p_until") OffsetDateTime until,
            @Param("p_category_id") Integer categoryId
    );

    @Query(value = "SELECT * FROM fn_reporte_inventario(:p_category_id, :p_status_stock, :p_busqueda, :p_publisher_id, :p_supplier_id, :p_status_book_id, :p_language_id, :p_year_from, :p_year_until, :p_stock_total_min, :p_stock_total_max, :p_stock_disp_min, :p_stock_disp_max, :p_location)",
            nativeQuery = true)
    List<ReportInventoryProjection> fnReportInventory(
            @Param("p_category_id") Integer categoryId,
            @Param("p_status_stock") String statusStock,
            @Param("p_busqueda") String busqueda,
            @Param("p_publisher_id") Integer publisherId,
            @Param("p_supplier_id") Integer supplierId,
            @Param("p_status_book_id") Integer statusBookId,
            @Param("p_language_id") Integer languageId,
            @Param("p_year_from") Short yearFrom,
            @Param("p_year_until") Short yearUntil,
            @Param("p_stock_total_min") Short stockTotalMin,
            @Param("p_stock_total_max") Short stockTotalMax,
            @Param("p_stock_disp_min") Short stockDispMin,
            @Param("p_stock_disp_max") Short stockDispMax,
            @Param("p_location") String location
    );

    @Query(value = "SELECT * FROM fn_reporte_inventario(:p_category_id, :p_status_stock, :p_busqueda, :p_publisher_id, :p_supplier_id, :p_status_book_id, :p_language_id, :p_year_from, :p_year_until, :p_stock_total_min, :p_stock_total_max, :p_stock_disp_min, :p_stock_disp_max, :p_location) LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<ReportInventoryProjection> fnReportInventoryPaginated(
            @Param("p_category_id") Integer categoryId,
            @Param("p_status_stock") String statusStock,
            @Param("p_busqueda") String busqueda,
            @Param("p_publisher_id") Integer publisherId,
            @Param("p_supplier_id") Integer supplierId,
            @Param("p_status_book_id") Integer statusBookId,
            @Param("p_language_id") Integer languageId,
            @Param("p_year_from") Short yearFrom,
            @Param("p_year_until") Short yearUntil,
            @Param("p_stock_total_min") Short stockTotalMin,
            @Param("p_stock_total_max") Short stockTotalMax,
            @Param("p_stock_disp_min") Short stockDispMin,
            @Param("p_stock_disp_max") Short stockDispMax,
            @Param("p_location") String location,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_inventario(:p_category_id, :p_status_stock, :p_busqueda, :p_publisher_id, :p_supplier_id, :p_status_book_id, :p_language_id, :p_year_from, :p_year_until, :p_stock_total_min, :p_stock_total_max, :p_stock_disp_min, :p_stock_disp_max, :p_location)",
            nativeQuery = true)
    long countReportInventory(
            @Param("p_category_id") Integer categoryId,
            @Param("p_status_stock") String statusStock,
            @Param("p_busqueda") String busqueda,
            @Param("p_publisher_id") Integer publisherId,
            @Param("p_supplier_id") Integer supplierId,
            @Param("p_status_book_id") Integer statusBookId,
            @Param("p_language_id") Integer languageId,
            @Param("p_year_from") Short yearFrom,
            @Param("p_year_until") Short yearUntil,
            @Param("p_stock_total_min") Short stockTotalMin,
            @Param("p_stock_total_max") Short stockTotalMax,
            @Param("p_stock_disp_min") Short stockDispMin,
            @Param("p_stock_disp_max") Short stockDispMax,
            @Param("p_location") String location
    );

    @Query(value = "SELECT * FROM fn_reporte_prestamos_vencidos(CAST(:p_days_atraso_min AS INTEGER), CAST(:p_busqueda AS TEXT), CAST(:p_days_atraso_max AS INTEGER))",
            nativeQuery = true)
    List<ReportOverduesProjection> fnReportLoansOverdues(
            @Param("p_days_atraso_min") Integer daysAtrasoMin,
            @Param("p_busqueda") String busqueda,
            @Param("p_days_atraso_max") Integer daysAtrasoMax
    );

    // Compat 2 params (existente): delega a 3 params con null en max (evita romper callers)
    default List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return fnReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    @Query(value = "SELECT * FROM fn_reporte_prestamos_vencidos(CAST(:p_days_atraso_min AS INTEGER), CAST(:p_busqueda AS TEXT), CAST(:p_days_atraso_max AS INTEGER)) LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<ReportOverduesProjection> fnReportLoansOverduesPaginated(
            @Param("p_days_atraso_min") Integer daysAtrasoMin,
            @Param("p_busqueda") String busqueda,
            @Param("p_days_atraso_max") Integer daysAtrasoMax,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    default List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin, String busqueda, int limit, int offset) {
        return fnReportLoansOverduesPaginated(daysAtrasoMin, busqueda, null, limit, offset);
    }

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_prestamos_vencidos(CAST(:p_days_atraso_min AS INTEGER), CAST(:p_busqueda AS TEXT), CAST(:p_days_atraso_max AS INTEGER))",
            nativeQuery = true)
    long countReportLoansOverdues(
            @Param("p_days_atraso_min") Integer daysAtrasoMin,
            @Param("p_busqueda") String busqueda,
            @Param("p_days_atraso_max") Integer daysAtrasoMax
    );

    default long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return countReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    @Query(value = "SELECT * FROM fn_reporte_categorias_demandadas(:p_limit, :p_from, :p_until)",
            nativeQuery = true)
    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemanded(
            @Param("p_limit") Integer maxLimit,
            @Param("p_from") OffsetDateTime from,
            @Param("p_until") OffsetDateTime until
    );

    // Paginated wrappers restantes (wrapper LIMIT/OFFSET + COUNT) — codigo legible, una query por reporte
    @Query(value = "SELECT * FROM fn_reporte_categorias_demandadas(:p_limit, :p_from, :p_until) LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemandedPaginated(
            @Param("p_limit") Integer maxLimit, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until,
            @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_categorias_demandadas(:p_limit, :p_from, :p_until)", nativeQuery = true)
    long countReportCategoriesDemanded(@Param("p_limit") Integer maxLimit, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until);

    @Query(value = "SELECT * FROM fn_reporte_indice_morosidad(:p_limit) LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<ReportDelinquencyProjection> fnReportIndexDelinquencyPaginated(@Param("p_limit") Integer maxLimit, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_indice_morosidad(:p_limit)", nativeQuery = true)
    long countReportIndexDelinquency(@Param("p_limit") Integer maxLimit);

    @Query(value = "SELECT * FROM fn_reporte_libros_mas_prestados_detallado(:p_limit, :p_from, :p_until, :p_category_id) LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<BookMostLoanedDetailedProjection> fnReportBooksDetailedPaginated(
            @Param("p_limit") Integer maxLimit, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until,
            @Param("p_category_id") Integer categoryId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_libros_mas_prestados_detallado(:p_limit, :p_from, :p_until, :p_category_id)", nativeQuery = true)
    long countReportBooksDetailed(@Param("p_limit") Integer maxLimit, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until, @Param("p_category_id") Integer categoryId);

    @Query(value = "SELECT * FROM fn_reporte_uso_por_periodo(:p_granularidad, :p_from, :p_until) LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<ReportUsageByPeriodProjection> fnReportUsageByPeriodPaginated(
            @Param("p_granularidad") String granularidad, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until,
            @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM fn_reporte_uso_por_periodo(:p_granularidad, :p_from, :p_until)", nativeQuery = true)
    long countReportUsageByPeriod(@Param("p_granularidad") String granularidad, @Param("p_from") OffsetDateTime from, @Param("p_until") OffsetDateTime until);
}
