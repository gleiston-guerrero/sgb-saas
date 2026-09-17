package com.uteq.backend.repository;

import com.uteq.backend.repository.projection.BookMostLoanedDetailedProjection;
import com.uteq.backend.repository.projection.BookMostLoanedProjection;
import com.uteq.backend.repository.projection.LoanActiveBaseProjection;
import com.uteq.backend.repository.projection.ReportCategoriesDemandedProjection;
import com.uteq.backend.repository.projection.ReportDelinquencyProjection;
import com.uteq.backend.repository.projection.ReportInventoryProjection;
import com.uteq.backend.repository.projection.ReportOverduesProjection;
import com.uteq.backend.repository.projection.ReportUsageByPeriodProjection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fragmento custom para stored procedures de prestamos (P5:
 * StoredProcedureQuery posicional) y reportes tabulares reimplementados
 * sin SQL nativo (Criteria/JPQL + cómputo Java con semántica idéntica a
 * cada función fn_*; las funciones quedan en BD para psql directo).
 */
public interface LoanProcedureRepositoryCustom {
    /**
     * Crea un préstamo mediante {@code proc_crear_prestamo} tras validar stock y bloqueo del lector.
     *
     * @param userId identificador del lector solicitante
     * @param bookId identificador del libro a prestar
     * @param librarianId identificador del bibliotecario que registra
     * @param daysLoan plazo en días para la devolución estimada
     * @return identificador del préstamo creado
     */
    Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan);
    /**
     * Registra la devolución mediante {@code proc_registrar_devolucion}; restaura stock y multa si hay atraso.
     *
     * @param loanId identificador del préstamo devuelto
     * @return mapa con {@code o_prestamo_id}, {@code o_hubo_multa} y {@code o_monto_multa}
     */
    Map<String, Object> spRegisterLoanReturn(Long loanId);

    /**
     * Réplica de {@code fn_listar_prestamos_activos_por_usuario}: préstamos no DEVUELTOs del usuario
     * por fecha estimada ascendente.
     *
     * @param userId identificador del lector
     * @return lista de préstamos activos del usuario
     */
    List<LoanActiveBaseProjection> fnListLoansActivesByUser(Long userId);

    /**
     * Réplica de {@code fn_reporte_libros_mas_prestados}: ranking de libros por total de préstamos
     * en el rango, de mayor a menor.
     *
     * @param maxLimit tope de filas, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return ranking de libros más prestados
     */
    List<BookMostLoanedProjection> fnReportBooksMostLoaned(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until);

    /**
     * Réplica de {@code fn_reporte_indice_morosidad}: lectores con multas PENDIENTEs agregados
     * por deuda descendente, limitado al tope.
     *
     * @param maxLimit tope de lectores, nulo = 10
     * @return lista de lectores morosos
     */
    List<ReportDelinquencyProjection> fnReportIndexDelinquency(Integer maxLimit);
    /**
     * Página del reporte de morosidad sobre el ranking limitado por {@code maxLimit}.
     *
     * @param maxLimit tope de lectores, nulo = 10
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del ranking
     * @return página de lectores morosos
     */
    List<ReportDelinquencyProjection> fnReportIndexDelinquencyPaginated(
            Integer maxLimit, int limit, int offset);
    /**
     * Total de filas del reporte de morosidad con el tope dado.
     *
     * @param maxLimit tope de lectores, nulo = 10
     * @return cantidad de lectores en el ranking
     */
    long countReportIndexDelinquency(Integer maxLimit);

    /**
     * Réplica de {@code fn_reporte_uso_por_periodo}: préstamos y devoluciones agregados por período.
     *
     * @param granularidad agrupación ({@code dia}, {@code semana} o {@code mes}), nulo = día
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return lista de períodos con ambos conteos
     */
    List<ReportUsageByPeriodProjection> fnReportUsageByPeriod(String granularidad,
            OffsetDateTime from, OffsetDateTime until);
    /**
     * Página del reporte de uso por período.
     *
     * @param granularidad agrupación ({@code dia}, {@code semana} o {@code mes}), nulo = día
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del reporte
     * @return página de períodos con ambos conteos
     */
    List<ReportUsageByPeriodProjection> fnReportUsageByPeriodPaginated(String granularidad,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset);
    /**
     * Total de períodos del reporte de uso con los filtros dados.
     *
     * @param granularidad agrupación ({@code dia}, {@code semana} o {@code mes}), nulo = día
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return cantidad de períodos
     */
    long countReportUsageByPeriod(String granularidad, OffsetDateTime from, OffsetDateTime until);

    /**
     * Réplica de {@code fn_reporte_libros_mas_prestados_detallado}: ranking con autores/categorías
     * agregados y porcentaje sobre el total, con filtro opcional de categoría.
     *
     * @param maxLimit tope de filas, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param categoryId categoría exigida, nulo = todas
     * @return ranking detallado de libros más prestados
     */
    List<BookMostLoanedDetailedProjection> fnReportBooksMostLoanedDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId);
    /**
     * Página del reporte detallado de libros más prestados.
     *
     * @param maxLimit tope de filas, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param categoryId categoría exigida, nulo = todas
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del ranking
     * @return página del ranking detallado
     */
    List<BookMostLoanedDetailedProjection> fnReportBooksDetailedPaginated(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId, int limit, int offset);
    /**
     * Total de filas del reporte detallado de libros con los filtros dados.
     *
     * @param maxLimit tope de filas, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param categoryId categoría exigida, nulo = todas
     * @return cantidad de libros en el ranking
     */
    long countReportBooksDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId);

    /**
     * Réplica de {@code fn_reporte_inventario}: existencias con 14 filtros opcionales y
     * disponibilidad calculada.
     *
     * @param categoryId categoría exigida, nulo = todas
     * @param statusStock disponibilidad ({@code agotado}, {@code baja} o {@code disponible}), nulo = todas
     * @param busqueda texto en título/ISBN/autor/editorial/proveedor, nulo = sin filtro
     * @param publisherId editorial exigida, nulo = todas
     * @param supplierId proveedor exigido, nulo = todos
     * @param statusBookId estado exigido, nulo = todos
     * @param languageId idioma exigido, nulo = todos
     * @param yearFrom año mínimo de publicación, nulo = sin mínimo
     * @param yearUntil año máximo de publicación, nulo = sin máximo
     * @param stockTotalMin stock total mínimo, nulo = sin mínimo
     * @param stockTotalMax stock total máximo, nulo = sin máximo
     * @param stockDispMin disponibles mínimos, nulo = sin mínimo
     * @param stockDispMax disponibles máximos, nulo = sin máximo
     * @param location texto en ubicación física, nulo = sin filtro
     * @return lista de existencias coincidentes
     */
    List<ReportInventoryProjection> fnReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location);
    /**
     * Página del reporte de inventario con los mismos 14 filtros.
     *
     * @param categoryId categoría exigida, nulo = todas
     * @param statusStock disponibilidad ({@code agotado}, {@code baja} o {@code disponible}), nulo = todas
     * @param busqueda texto en título/ISBN/autor/editorial/proveedor, nulo = sin filtro
     * @param publisherId editorial exigida, nulo = todas
     * @param supplierId proveedor exigido, nulo = todos
     * @param statusBookId estado exigido, nulo = todos
     * @param languageId idioma exigido, nulo = todos
     * @param yearFrom año mínimo de publicación, nulo = sin mínimo
     * @param yearUntil año máximo de publicación, nulo = sin máximo
     * @param stockTotalMin stock total mínimo, nulo = sin mínimo
     * @param stockTotalMax stock total máximo, nulo = sin máximo
     * @param stockDispMin disponibles mínimos, nulo = sin mínimo
     * @param stockDispMax disponibles máximos, nulo = sin máximo
     * @param location texto en ubicación física, nulo = sin filtro
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del reporte
     * @return página de existencias coincidentes
     */
    List<ReportInventoryProjection> fnReportInventoryPaginated(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location,
            int limit, int offset);
    /**
     * Total de filas del reporte de inventario con los mismos 14 filtros.
     *
     * @param categoryId categoría exigida, nulo = todas
     * @param statusStock disponibilidad ({@code agotado}, {@code baja} o {@code disponible}), nulo = todas
     * @param busqueda texto en título/ISBN/autor/editorial/proveedor, nulo = sin filtro
     * @param publisherId editorial exigida, nulo = todas
     * @param supplierId proveedor exigido, nulo = todos
     * @param statusBookId estado exigido, nulo = todos
     * @param languageId idioma exigido, nulo = todos
     * @param yearFrom año mínimo de publicación, nulo = sin mínimo
     * @param yearUntil año máximo de publicación, nulo = sin máximo
     * @param stockTotalMin stock total mínimo, nulo = sin mínimo
     * @param stockTotalMax stock total máximo, nulo = sin máximo
     * @param stockDispMin disponibles mínimos, nulo = sin mínimo
     * @param stockDispMax disponibles máximos, nulo = sin máximo
     * @param location texto en ubicación física, nulo = sin filtro
     * @return cantidad de existencias coincidentes
     */
    long countReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location);

    /**
     * Réplica de {@code fn_reporte_prestamos_vencidos}: préstamos ACTIVO/RENOVADO vencidos con
     * días de atraso y multa estimada, por atraso descendente.
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @param daysAtrasoMax atraso máximo en días, nulo = sin máximo
     * @return lista de préstamos vencidos
     */
    List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin, String busqueda,
            Integer daysAtrasoMax);

    /**
     * Variante sin tope de atraso (equivale a {@code daysAtrasoMax} nulo).
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @return lista de préstamos vencidos
     */
    default List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return fnReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    /**
     * Página del reporte de préstamos vencidos.
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @param daysAtrasoMax atraso máximo en días, nulo = sin máximo
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del reporte
     * @return página de préstamos vencidos
     */
    List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin,
            String busqueda, Integer daysAtrasoMax, int limit, int offset);

    /**
     * Variante sin tope de atraso (equivale a {@code daysAtrasoMax} nulo).
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del reporte
     * @return página de préstamos vencidos
     */
    default List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin,
            String busqueda, int limit, int offset) {
        return fnReportLoansOverduesPaginated(daysAtrasoMin, busqueda, null, limit, offset);
    }

    /**
     * Total de préstamos vencidos con los filtros dados.
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @param daysAtrasoMax atraso máximo en días, nulo = sin máximo
     * @return cantidad de préstamos vencidos
     */
    long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda, Integer daysAtrasoMax);

    /**
     * Variante sin tope de atraso (equivale a {@code daysAtrasoMax} nulo).
     *
     * @param daysAtrasoMin atraso mínimo en días, nulo = sin mínimo
     * @param busqueda texto en lector/libro, nulo = sin filtro
     * @return cantidad de préstamos vencidos
     */
    default long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return countReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    /**
     * Réplica de {@code fn_reporte_categorias_demandadas}: categorías por total de préstamos
     * con porcentaje sobre el total, de mayor a menor.
     *
     * @param maxLimit tope de filas (la función lo ignora), nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return ranking de categorías demandadas
     */
    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemanded(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until);
    /**
     * Página del reporte de categorías demandadas.
     *
     * @param maxLimit tope de filas (la función lo ignora), nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param limit tamaño de la página
     * @param offset desplazamiento dentro del ranking
     * @return página del ranking de categorías
     */
    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemandedPaginated(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset);
    /**
     * Total de categorías del reporte con los filtros dados.
     *
     * @param maxLimit tope de filas (la función lo ignora), nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return cantidad de categorías en el ranking
     */
    long countReportCategoriesDemanded(Integer maxLimit, OffsetDateTime from, OffsetDateTime until);
}
