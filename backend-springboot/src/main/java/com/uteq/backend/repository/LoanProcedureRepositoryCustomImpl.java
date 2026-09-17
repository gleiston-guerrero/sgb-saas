package com.uteq.backend.repository;

import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.Fine;
import com.uteq.backend.entity.Loan;
import com.uteq.backend.entity.StatusFine;
import com.uteq.backend.entity.StatusLoan;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.projection.BookMostLoanedDetailedProjection;
import com.uteq.backend.repository.projection.BookMostLoanedProjection;
import com.uteq.backend.repository.projection.LoanActiveBaseProjection;
import com.uteq.backend.repository.projection.ReportCategoriesDemandedProjection;
import com.uteq.backend.repository.projection.ReportDelinquencyProjection;
import com.uteq.backend.repository.projection.ReportInventoryProjection;
import com.uteq.backend.repository.projection.ReportOverduesProjection;
import com.uteq.backend.repository.projection.ReportUsageByPeriodProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Repository
class LoanProcedureRepositoryCustomImpl implements LoanProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ConfigurationSystemRepository configuracion;

    private static final SpelAwareProxyProjectionFactory PROYECCIONES =
            new SpelAwareProxyProjectionFactory();

    /**
     * Creates a loan through the stored procedure proc_crear_prestamo
     * (CREATE PROCEDURE nativo de V51, invocado con CALL), que a su vez
     * envuelve la función sp_crear_prestamo -- valida stock disponible y
     * bloqueo por multas del lector.
     *
     * <p>Se invoca con {@code StoredProcedureQuery} JPA y binding
     * exclusivamente posicional (sin nombres de parámetro) porque el proxy
     * estándar de {@code @Procedure}/{@code @NamedStoredProcedureQuery} de
     * Hibernate 6 genera sintaxis de parámetros nombrados de PostgreSQL
     * dentro del escape JDBC {@code {call ...}}, que pgjdbc rechaza
     * (spring-projects/spring-data-jpa#3393). Sin SQL nativo: P5.
     *
     * @param userId identifier of the lector requesting the loan
     * @param bookId identifier of the book to loan
     * @param librarianId identifier of the librarian registering the loan
     * @param daysLoan loan term in days used to compute the estimated return date
     * @return identifier of the loan created by the procedure
     */
    @Override
    public Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_crear_prestamo");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(4, Integer.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(5, Long.class, ParameterMode.OUT);
        sp.setParameter(1, userId);
        sp.setParameter(2, bookId);
        sp.setParameter(3, librarianId);
        sp.setParameter(4, daysLoan);
        sp.execute();
        return (Long) sp.getOutputParameterValue(5);
    }

    /**
     * Registers a loan return through the stored procedure
     * proc_registrar_devolucion (CREATE PROCEDURE nativo de V51, invocado
     * con CALL), que envuelve sp_registrar_devolucion -- restaura stock y
     * genera multa automática si la devolución es tardía. Mismo criterio de
     * binding posicional que {@link #spCreateLoanProcedure} arriba.
     *
     * @param loanId identifier of the loan being returned
     * @return map with o_prestamo_id (returned loan id), o_hubo_multa (whether
     *         an overdue fine was generated) and o_monto_multa (fine amount, if any)
     */
    @Override
    public Map<String, Object> spRegisterLoanReturn(Long loanId) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_registrar_devolucion");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(3, Boolean.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(4, BigDecimal.class, ParameterMode.OUT);
        sp.setParameter(1, loanId);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_prestamo_id", (Long) sp.getOutputParameterValue(2));
        result.put("o_hubo_multa", (Boolean) sp.getOutputParameterValue(3));
        result.put("o_monto_multa", (BigDecimal) sp.getOutputParameterValue(4));
        return result;
    }

    // ── Reportes tabulares (P5, sin SQL nativo) ──────────────────────────
    // Cada método replica la función fn_* indicada: mismos filtros, orden,
    // límites y redondeos. Las funciones quedan en BD para psql directo.

    @Override
    public List<LoanActiveBaseProjection> fnListLoansActivesByUser(Long userId) {
        List<Object[]> filas = em.createQuery("""
                SELECT p.id, b.title, b.isbn,
                       p.dateLoan, p.dateLoanReturnEstimada, s.name
                FROM Loan p, Book b, StatusLoan s
                WHERE b.id = p.bookId
                  AND s.id = p.statusLoanId
                  AND p.userId = :userId
                  AND s.name <> 'DEVUELTO'
                ORDER BY p.dateLoanReturnEstimada ASC""", Object[].class)
                .setParameter("userId", userId)
                .getResultList();
        List<LoanActiveBaseProjection> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            resultado.add(proyectar(LoanActiveBaseProjection.class,
                    "loanId", fila[0], "bookTitle", fila[1], "bookIsbn", fila[2],
                    "dateLoan", fila[3], "dateLoanReturnEstimada", fila[4],
                    "statusName", fila[5]));
        }
        return resultado;
    }

    @Override
    public List<BookMostLoanedProjection> fnReportBooksMostLoaned(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Loan> prestamo = query.from(Loan.class);
        Root<Book> libro = query.from(Book.class);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(libro.get("id"), prestamo.get("bookId")));
        if (from != null) {
            filtros.add(cb.greaterThanOrEqualTo(prestamo.get("dateLoan"), from));
        }
        if (until != null) {
            filtros.add(cb.lessThanOrEqualTo(prestamo.get("dateLoan"), until));
        }
        query.multiselect(libro.get("id"), libro.get("title"), libro.get("isbn"),
                cb.count(prestamo));
        query.where(filtros.toArray(Predicate[]::new));
        query.groupBy(libro.get("id"), libro.get("title"), libro.get("isbn"));
        query.orderBy(cb.desc(cb.count(prestamo)));
        List<Object[]> filas = em.createQuery(query).getResultList();
        List<BookMostLoanedProjection> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            resultado.add(proyectar(BookMostLoanedProjection.class,
                    "bookId", fila[0], "title", fila[1], "isbn", fila[2],
                    "totalLoans", fila[3]));
        }
        return limitar(resultado, maxLimit, true);
    }

    @Override
    public List<ReportDelinquencyProjection> fnReportIndexDelinquency(Integer maxLimit) {
        return morosidad(null, null, null, null, null, maxLimit, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<ReportDelinquencyProjection> fnReportIndexDelinquencyPaginated(
            Integer maxLimit, int limit, int offset) {
        return morosidad(null, null, null, null, null, maxLimit, limit, offset).content;
    }

    @Override
    public long countReportIndexDelinquency(Integer maxLimit) {
        return morosidad(null, null, null, null, null, maxLimit, Integer.MAX_VALUE, 0).total;
    }

    private record Pagina<T>(List<T> content, long total) {
    }

    /**
     * Réplica de fn_reporte_indice_morosidad (V46): agrega multas PENDIENTE
     * por lector con SUM/COUNT y atraso promedio
     * ROUND(AVG(GREATEST(0, días))) a 1 decimal, orden por deuda DESC.
     * Los filtros de búsqueda/rangos de la sobrecarga V45 (dropeada en V46)
     * no aplican: la firma vigente recibe solo el límite.
     */
    private Pagina<ReportDelinquencyProjection> morosidad(String busqueda, BigDecimal montoMin,
            BigDecimal montoMax, BigDecimal diasMin, BigDecimal diasMax,
            Integer maxLimit, int limit, int offset) {
        int tope = (maxLimit != null) ? maxLimit : 10;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Fine> multa = query.from(Fine.class);
        Root<StatusFine> estado = query.from(StatusFine.class);
        Root<Loan> prestamo = query.from(Loan.class);
        Root<User> usuario = query.from(User.class);
        query.multiselect(usuario.get("id"), usuario.get("name"), usuario.get("lastName"),
                usuario.get("email"), multa.get("amount"),
                prestamo.get("dateLoanReturnEstimada"), prestamo.get("dateLoanReturnReal"));
        query.where(
                cb.equal(estado.get("id"), multa.get("statusFineId")),
                cb.equal(estado.get("name"), "PENDIENTE"),
                cb.equal(prestamo.get("id"), multa.get("loanId")),
                cb.equal(usuario.get("id"), prestamo.get("userId")));
        OffsetDateTime ahora = OffsetDateTime.now();
        Map<Long, BigDecimal> sumas = new HashMap<>();
        Map<Long, Long> conteos = new HashMap<>();
        Map<Long, Long> diasSum = new HashMap<>();
        Map<Long, String[]> nombres = new HashMap<>();
        for (Object[] fila : em.createQuery(query).getResultList()) {
            Long uid = (Long) fila[0];
            nombres.putIfAbsent(uid, new String[]{(String) fila[1], (String) fila[2], (String) fila[3]});
            sumas.merge(uid, (BigDecimal) fila[4], BigDecimal::add);
            conteos.merge(uid, 1L, Long::sum);
            OffsetDateTime estimada = (OffsetDateTime) fila[5];
            OffsetDateTime real = (OffsetDateTime) fila[6];
            long dias = Math.max(0L, Duration.between(estimada.toInstant(),
                    (real != null ? real : ahora).toInstant()).toDays());
            diasSum.merge(uid, dias, Long::sum);
        }
        record Fila(Long userId, String name, String lastName, String email,
                    BigDecimal total, long n, BigDecimal promedio) {
        }
        List<Fila> filas = new ArrayList<>();
        for (Long uid : sumas.keySet()) {
            BigDecimal total = sumas.get(uid).setScale(2, RoundingMode.HALF_UP);
            long n = conteos.get(uid);
            BigDecimal promedio = BigDecimal.valueOf(diasSum.get(uid))
                    .divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP)
                    .setScale(1, RoundingMode.HALF_UP);
            String[] nm = nombres.get(uid);
            filas.add(new Fila(uid, nm[0], nm[1], nm[2], total, n, promedio));
        }
        filas.sort(Comparator.comparing(Fila::total).reversed());
        List<Fila> limitadas = filas.subList(0, Math.min(tope, filas.size()));
        List<ReportDelinquencyProjection> contenido = new ArrayList<>();
        for (Fila f : paginar(limitadas, limit, offset)) {
            contenido.add(proyectar(ReportDelinquencyProjection.class,
                    "userId", f.userId(), "name", f.name(), "lastName", f.lastName(),
                    "email", f.email(), "amountTotalAdeudado", f.total(),
                    "quantityFinesPendientes", f.n(), "daysAtrasoPromedio", f.promedio()));
        }
        return new Pagina<>(contenido, limitadas.size());
    }

    @Override
    public List<ReportUsageByPeriodProjection> fnReportUsageByPeriod(String granularidad,
            OffsetDateTime from, OffsetDateTime until) {
        return usoPorPeriodo(granularidad, from, until, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<ReportUsageByPeriodProjection> fnReportUsageByPeriodPaginated(String granularidad,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset) {
        return usoPorPeriodo(granularidad, from, until, limit, offset).content;
    }

    @Override
    public long countReportUsageByPeriod(String granularidad, OffsetDateTime from, OffsetDateTime until) {
        return usoPorPeriodo(granularidad, from, until, Integer.MAX_VALUE, 0).total;
    }

    /**
     * Réplica de fn_reporte_uso_por_periodo: agrega préstamos y devoluciones
     * por período (dia/semana/mes) con FULL OUTER JOIN lógico en Java.
     */
    private Pagina<ReportUsageByPeriodProjection> usoPorPeriodo(String granularidad,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset) {
        String campo = switch (granularidad != null ? granularidad : "dia") {
            case "semana" -> "week";
            case "mes" -> "month";
            default -> "day";
        };
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> qPrestamos = cb.createQuery(Object[].class);
        Root<Loan> p1 = qPrestamos.from(Loan.class);
        var truncP = cb.function("date_trunc", OffsetDateTime.class,
                cb.literal(campo), p1.get("dateLoan"));
        List<Predicate> f1 = new ArrayList<>();
        if (from != null) {
            f1.add(cb.greaterThanOrEqualTo(p1.get("dateLoan"), from));
        }
        if (until != null) {
            f1.add(cb.lessThanOrEqualTo(p1.get("dateLoan"), until));
        }
        qPrestamos.multiselect(truncP, cb.count(p1)).where(f1.toArray(Predicate[]::new)).groupBy(truncP);

        CriteriaQuery<Object[]> qDev = cb.createQuery(Object[].class);
        Root<Loan> p2 = qDev.from(Loan.class);
        var truncD = cb.function("date_trunc", OffsetDateTime.class,
                cb.literal(campo), p2.get("dateLoanReturnReal"));
        List<Predicate> f2 = new ArrayList<>();
        f2.add(cb.isNotNull(p2.get("dateLoanReturnReal")));
        if (from != null) {
            f2.add(cb.greaterThanOrEqualTo(p2.get("dateLoanReturnReal"), from));
        }
        if (until != null) {
            f2.add(cb.lessThanOrEqualTo(p2.get("dateLoanReturnReal"), until));
        }
        qDev.multiselect(truncD, cb.count(p2)).where(f2.toArray(Predicate[]::new)).groupBy(truncD);

        Map<Instant, long[]> periodos = new TreeMap<>();
        for (Object[] fila : em.createQuery(qPrestamos).getResultList()) {
            periodos.computeIfAbsent(((OffsetDateTime) fila[0]).toInstant(), k -> new long[2])[0] =
                    (Long) fila[1];
        }
        for (Object[] fila : em.createQuery(qDev).getResultList()) {
            periodos.computeIfAbsent(((OffsetDateTime) fila[0]).toInstant(), k -> new long[2])[1] =
                    (Long) fila[1];
        }
        List<ReportUsageByPeriodProjection> todas = new ArrayList<>();
        for (Map.Entry<Instant, long[]> e : periodos.entrySet()) {
            todas.add(proyectar(ReportUsageByPeriodProjection.class,
                    "period", e.getKey(), "totalLoans", e.getValue()[0],
                    "totalLoanReturns", e.getValue()[1]));
        }
        List<ReportUsageByPeriodProjection> pagina = paginar(todas, limit, offset);
        return new Pagina<>(pagina, todas.size());
    }

    @Override
    public List<BookMostLoanedDetailedProjection> fnReportBooksMostLoanedDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId) {
        return librosDetallado(maxLimit, from, until, categoryId, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<BookMostLoanedDetailedProjection> fnReportBooksDetailedPaginated(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId, int limit, int offset) {
        return librosDetallado(maxLimit, from, until, categoryId, limit, offset).content;
    }

    @Override
    public long countReportBooksDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId) {
        return librosDetallado(maxLimit, from, until, categoryId, Integer.MAX_VALUE, 0).total;
    }

    /**
     * Réplica de fn_reporte_libros_mas_prestados_detallado: ranking con
     * autores/categorías agregados (DISTINCT ordenado) y porcentaje sobre
     * el total general con los mismos filtros.
     */
    private Pagina<BookMostLoanedDetailedProjection> librosDetallado(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId, int limit, int offset) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Loan> prestamo = query.from(Loan.class);
        Root<Book> libro = query.from(Book.class);
        var autores = libro.join("authors", JoinType.LEFT);
        var categorias = libro.join("categories", JoinType.LEFT);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(libro.get("id"), prestamo.get("bookId")));
        if (from != null) {
            filtros.add(cb.greaterThanOrEqualTo(prestamo.get("dateLoan"), from));
        }
        if (until != null) {
            filtros.add(cb.lessThanOrEqualTo(prestamo.get("dateLoan"), until));
        }
        if (categoryId != null) {
            var sub = query.subquery(Long.class);
            var l2 = sub.from(Book.class);
            var c2 = l2.join("categories");
            sub.select(l2.get("id")).where(cb.equal(c2.get("id"), categoryId));
            filtros.add(prestamo.get("bookId").in(sub));
        }
        query.multiselect(libro.get("id"), libro.get("title"), libro.get("isbn"),
                autores.get("name"), categorias.get("name"));
        query.where(filtros.toArray(Predicate[]::new));
        record Clave(Long id, String titulo, String isbn) {
        }
        Map<Clave, Long> conteos = new HashMap<>();
        Map<Clave, TreeSet<String>> autoresPorLibro = new HashMap<>();
        Map<Clave, TreeSet<String>> catsPorLibro = new HashMap<>();
        long totalGeneral = 0;
        for (Object[] fila : em.createQuery(query).getResultList()) {
            Clave clave = new Clave((Long) fila[0], (String) fila[1], (String) fila[2]);
            conteos.merge(clave, 1L, Long::sum);
            totalGeneral++;
            if (fila[3] != null) {
                autoresPorLibro.computeIfAbsent(clave, k -> new TreeSet<>()).add((String) fila[3]);
            }
            if (fila[4] != null) {
                catsPorLibro.computeIfAbsent(clave, k -> new TreeSet<>()).add((String) fila[4]);
            }
        }
        record Fila(Clave clave, long total) {
        }
        List<Fila> filas = new ArrayList<>();
        for (Map.Entry<Clave, Long> e : conteos.entrySet()) {
            filas.add(new Fila(e.getKey(), e.getValue()));
        }
        filas.sort(Comparator.comparingLong(Fila::total).reversed());
        List<BookMostLoanedDetailedProjection> todas = new ArrayList<>();
        for (Fila f : filas) {
            todas.add(proyectar(BookMostLoanedDetailedProjection.class,
                    "bookId", f.clave().id(), "title", f.clave().titulo(),
                    "isbn", f.clave().isbn(),
                    "authorName", textoAgregado(autoresPorLibro.get(f.clave()), "Sin autor"),
                    "categoryName", textoAgregado(catsPorLibro.get(f.clave()), "Sin categoria"),
                    "totalLoans", f.total(), "percentage", porcentaje(f.total(), totalGeneral)));
        }
        List<BookMostLoanedDetailedProjection> pagina = paginar(todas, limit, offset);
        return new Pagina<>(pagina, todas.size());
    }

    @Override
    public List<ReportInventoryProjection> fnReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location) {
        return inventario(categoryId, statusStock, busqueda, publisherId, supplierId, statusBookId,
                languageId, yearFrom, yearUntil, stockTotalMin, stockTotalMax, stockDispMin,
                stockDispMax, location, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<ReportInventoryProjection> fnReportInventoryPaginated(Integer categoryId,
            String statusStock, String busqueda, Integer publisherId, Integer supplierId,
            Integer statusBookId, Integer languageId, Short yearFrom, Short yearUntil,
            Short stockTotalMin, Short stockTotalMax, Short stockDispMin, Short stockDispMax,
            String location, int limit, int offset) {
        return inventario(categoryId, statusStock, busqueda, publisherId, supplierId, statusBookId,
                languageId, yearFrom, yearUntil, stockTotalMin, stockTotalMax, stockDispMin,
                stockDispMax, location, limit, offset).content;
    }

    @Override
    public long countReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location) {
        return inventario(categoryId, statusStock, busqueda, publisherId, supplierId, statusBookId,
                languageId, yearFrom, yearUntil, stockTotalMin, stockTotalMax, stockDispMin,
                stockDispMax, location, Integer.MAX_VALUE, 0).total;
    }

    /**
     * Réplica de fn_reporte_inventario (V44): 14 filtros con LEFT JOINs,
     * autores/categorías agregados y estado de disponibilidad calculado.
     */
    private Pagina<ReportInventoryProjection> inventario(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location,
            int limit, int offset) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Book> libro = query.from(Book.class);
        var autores = libro.join("authors", JoinType.LEFT);
        var categorias = libro.join("categories", JoinType.LEFT);
        var editorial = libro.join("publisher", JoinType.LEFT);
        var proveedor = libro.join("supplier", JoinType.LEFT);
        var idioma = libro.join("language", JoinType.LEFT);
        var estado = libro.join("status", JoinType.LEFT);
        List<Predicate> filtros = new ArrayList<>();
        if (categoryId != null) {
            filtros.add(cb.equal(categorias.get("id"), categoryId));
        }
        if (publisherId != null) {
            filtros.add(cb.equal(libro.get("publisher").get("id"), publisherId));
        }
        if (supplierId != null) {
            filtros.add(cb.equal(libro.get("supplier").get("id"), supplierId));
        }
        if (statusBookId != null) {
            filtros.add(cb.equal(libro.get("status").get("id"), statusBookId));
        }
        if (languageId != null) {
            filtros.add(cb.equal(libro.get("language").get("id"), languageId));
        }
        if (yearFrom != null) {
            filtros.add(cb.greaterThanOrEqualTo(libro.get("yearPublication"), yearFrom));
        }
        if (yearUntil != null) {
            filtros.add(cb.lessThanOrEqualTo(libro.get("yearPublication"), yearUntil));
        }
        if (stockTotalMin != null) {
            filtros.add(cb.greaterThanOrEqualTo(libro.get("stockTotal"), stockTotalMin));
        }
        if (stockTotalMax != null) {
            filtros.add(cb.lessThanOrEqualTo(libro.get("stockTotal"), stockTotalMax));
        }
        if (stockDispMin != null) {
            filtros.add(cb.greaterThanOrEqualTo(libro.get("stockAvailable"), stockDispMin));
        }
        if (stockDispMax != null) {
            filtros.add(cb.lessThanOrEqualTo(libro.get("stockAvailable"), stockDispMax));
        }
        if (location != null) {
            filtros.add(ilike(cb, libro.get("locationPhysical"), location));
        }
        if (busqueda != null) {
            filtros.add(cb.or(
                    ilike(cb, libro.get("title"), busqueda),
                    ilike(cb, libro.get("isbn"), busqueda),
                    ilike(cb, autores.get("name"), busqueda),
                    ilike(cb, editorial.get("name"), busqueda),
                    ilike(cb, proveedor.get("name"), busqueda)));
        }
        if (statusStock != null) {
            filtros.add(switch (statusStock) {
                case "agotado" -> cb.equal(libro.get("stockAvailable"), (short) 0);
                case "baja" -> cb.equal(libro.get("stockAvailable"), (short) 1);
                case "disponible" -> cb.greaterThan(libro.get("stockAvailable"), (short) 1);
                default -> cb.disjunction();
            });
        }
        query.multiselect(libro.get("id"), libro.get("title"), libro.get("isbn"),
                autores.get("name"), categorias.get("name"),
                editorial.get("name"), proveedor.get("name"), idioma.get("name"),
                estado.get("name"), libro.get("yearPublication"), libro.get("locationPhysical"),
                libro.get("stockTotal"), libro.get("stockAvailable"));
        query.where(filtros.toArray(Predicate[]::new));
        record Clave(Long id, String titulo, String isbn, String editorial, String proveedor,
                     String idioma, String estado, Short anio, String ubicacion,
                     Short stockTotal, Short stockDisponible) {
        }
        Map<Clave, TreeSet<String>> autoresPorLibro = new LinkedHashMap<>();
        Map<Clave, TreeSet<String>> catsPorLibro = new LinkedHashMap<>();
        for (Object[] fila : em.createQuery(query).getResultList()) {
            Clave clave = new Clave((Long) fila[0], (String) fila[1], (String) fila[2],
                    (String) fila[5], (String) fila[6], (String) fila[7], (String) fila[8],
                    (Short) fila[9], (String) fila[10], (Short) fila[11], (Short) fila[12]);
            if (fila[3] != null) {
                autoresPorLibro.computeIfAbsent(clave, k -> new TreeSet<>()).add((String) fila[3]);
            } else {
                autoresPorLibro.putIfAbsent(clave, new TreeSet<>());
            }
            if (fila[4] != null) {
                catsPorLibro.computeIfAbsent(clave, k -> new TreeSet<>()).add((String) fila[4]);
            } else {
                catsPorLibro.putIfAbsent(clave, new TreeSet<>());
            }
        }
        record Fila(Clave clave) {
        }
        List<Clave> claves = new ArrayList<>(autoresPorLibro.keySet());
        claves.sort(Comparator.comparing(Clave::stockDisponible).thenComparing(Clave::titulo));
        List<ReportInventoryProjection> todas = new ArrayList<>();
        for (Clave c : claves) {
            todas.add(proyectar(ReportInventoryProjection.class,
                    "bookId", c.id(), "title", c.titulo(), "isbn", c.isbn(),
                    "authorName", textoAgregado(autoresPorLibro.get(c), "Sin autor"),
                    "categoryName", textoAgregado(catsPorLibro.get(c), "Sin categoria"),
                    "publisherName", c.editorial(), "supplierName", c.proveedor(),
                    "languageName", c.idioma(), "statusBookName", c.estado(),
                    "yearPublication", c.anio(), "locationPhysical", c.ubicacion(),
                    "stockTotal", c.stockTotal(), "stockAvailable", c.stockDisponible(),
                    "statusAvailability", estadoDisponibilidad(c.stockDisponible())));
        }
        List<ReportInventoryProjection> pagina = paginar(todas, limit, offset);
        return new Pagina<>(pagina, todas.size());
    }

    private String estadoDisponibilidad(Short stock) {
        if (stock == null || stock == 0) {
            return "Agotado";
        }
        if (stock == 1) {
            return "Baja disponibilidad";
        }
        return "Disponible";
    }

    @Override
    public List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin,
            String busqueda, Integer daysAtrasoMax) {
        return vencidos(daysAtrasoMin, busqueda, daysAtrasoMax, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin,
            String busqueda, Integer daysAtrasoMax, int limit, int offset) {
        return vencidos(daysAtrasoMin, busqueda, daysAtrasoMax, limit, offset).content;
    }

    @Override
    public long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda,
            Integer daysAtrasoMax) {
        return vencidos(daysAtrasoMin, busqueda, daysAtrasoMax, Integer.MAX_VALUE, 0).total;
    }

    /**
     * Réplica de fn_reporte_prestamos_vencidos (V45): préstamos ACTIVO/
     * RENOVADO vencidos con días de atraso y multa estimada
     * (días × monto_multa_diaria de configuración, 1 por defecto).
     */
    private Pagina<ReportOverduesProjection> vencidos(Integer daysAtrasoMin, String busqueda,
            Integer daysAtrasoMax, int limit, int offset) {
        BigDecimal tarifa = configuracion.findById("monto_multa_diaria")
                .map(c -> {
                    try {
                        return new BigDecimal(c.getValue());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ONE;
                    }
                }).orElse(BigDecimal.ONE);
        OffsetDateTime ahora = OffsetDateTime.now();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Loan> prestamo = query.from(Loan.class);
        Root<User> usuario = query.from(User.class);
        Root<Book> libro = query.from(Book.class);
        Root<StatusLoan> estado = query.from(StatusLoan.class);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(libro.get("id"), prestamo.get("bookId")));
        filtros.add(cb.equal(usuario.get("id"), prestamo.get("userId")));
        filtros.add(cb.equal(estado.get("id"), prestamo.get("statusLoanId")));
        filtros.add(estado.get("name").in("ACTIVO", "RENOVADO"));
        filtros.add(cb.lessThan(prestamo.get("dateLoanReturnEstimada"), ahora));
        if (busqueda != null) {
            filtros.add(cb.or(
                    ilike(cb, usuario.get("name"), busqueda),
                    ilike(cb, usuario.get("email"), busqueda),
                    ilike(cb, libro.get("title"), busqueda),
                    ilike(cb, libro.get("isbn"), busqueda)));
        }
        query.multiselect(prestamo.get("id"), usuario.get("name"), usuario.get("lastName"),
                usuario.get("email"), libro.get("title"), libro.get("isbn"),
                prestamo.get("dateLoanReturnEstimada"));
        query.where(filtros.toArray(Predicate[]::new));
        record Fila(Long loanId, String userName, String userEmail, String bookTitle,
                    String bookIsbn, OffsetDateTime estimada, long dias) {
        }
        List<Fila> filas = new ArrayList<>();
        for (Object[] fila : em.createQuery(query).getResultList()) {
            OffsetDateTime estimada = (OffsetDateTime) fila[6];
            long dias = Duration.between(estimada.toInstant(), ahora.toInstant()).toDays();
            if (daysAtrasoMin != null && dias < daysAtrasoMin) {
                continue;
            }
            if (daysAtrasoMax != null && dias > daysAtrasoMax) {
                continue;
            }
            filas.add(new Fila((Long) fila[0],
                    fila[1] + " " + fila[2], (String) fila[3], (String) fila[4],
                    (String) fila[5], estimada, dias));
        }
        filas.sort(Comparator.comparingLong(Fila::dias).reversed());
        List<ReportOverduesProjection> todas = new ArrayList<>();
        for (Fila f : filas) {
            todas.add(proyectar(ReportOverduesProjection.class,
                    "loanId", f.loanId(), "userName", f.userName(), "userEmail", f.userEmail(),
                    "bookTitle", f.bookTitle(), "bookIsbn", f.bookIsbn(),
                    "dateLoanReturnEstimada", f.estimada().toInstant(),
                    "daysAtraso", f.dias(),
                    "amountFineEstimada", BigDecimal.valueOf(f.dias()).multiply(tarifa)));
        }
        List<ReportOverduesProjection> pagina = paginar(todas, limit, offset);
        return new Pagina<>(pagina, todas.size());
    }

    @Override
    public List<ReportCategoriesDemandedProjection> fnReportCategoriesDemanded(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until) {
        return categorias(maxLimit, from, until, Integer.MAX_VALUE, 0).content;
    }

    @Override
    public List<ReportCategoriesDemandedProjection> fnReportCategoriesDemandedPaginated(
            Integer maxLimit, OffsetDateTime from, OffsetDateTime until, int limit, int offset) {
        return categorias(maxLimit, from, until, limit, offset).content;
    }

    @Override
    public long countReportCategoriesDemanded(Integer maxLimit, OffsetDateTime from,
            OffsetDateTime until) {
        return categorias(maxLimit, from, until, Integer.MAX_VALUE, 0).total;
    }

    /**
     * Réplica de fn_reporte_categorias_demandadas (V41, sin LIMIT interno:
     * el parámetro de límite se ignora igual que la función).
     */
    private Pagina<ReportCategoriesDemandedProjection> categorias(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Loan> prestamo = query.from(Loan.class);
        Root<Book> libro = query.from(Book.class);
        var rel = libro.join("categories");
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(libro.get("id"), prestamo.get("bookId")));
        if (from != null) {
            filtros.add(cb.greaterThanOrEqualTo(prestamo.get("dateLoan"), from));
        }
        if (until != null) {
            filtros.add(cb.lessThanOrEqualTo(prestamo.get("dateLoan"), until));
        }
        query.multiselect(rel.get("id"), rel.get("name"), cb.count(prestamo));
        query.where(filtros.toArray(Predicate[]::new));
        query.groupBy(rel.get("id"), rel.get("name"));
        query.orderBy(cb.desc(cb.count(prestamo)));
        CriteriaQuery<Long> qTotal = cb.createQuery(Long.class);
        Root<Loan> base = qTotal.from(Loan.class);
        List<Predicate> fTotal = new ArrayList<>();
        if (from != null) {
            fTotal.add(cb.greaterThanOrEqualTo(base.get("dateLoan"), from));
        }
        if (until != null) {
            fTotal.add(cb.lessThanOrEqualTo(base.get("dateLoan"), until));
        }
        qTotal.select(cb.count(base)).where(fTotal.toArray(Predicate[]::new));
        long totalGeneral = em.createQuery(qTotal).getSingleResult();
        record Fila(Integer id, String nombre, long total) {
        }
        List<Fila> filas = new ArrayList<>();
        for (Object[] fila : em.createQuery(query).getResultList()) {
            filas.add(new Fila((Integer) fila[0], (String) fila[1], (Long) fila[2]));
        }
        List<ReportCategoriesDemandedProjection> todas = new ArrayList<>();
        for (Fila f : filas) {
            todas.add(proyectar(ReportCategoriesDemandedProjection.class,
                    "categoryId", f.id(), "categoryName", f.nombre(),
                    "totalLoans", f.total(), "percentage", porcentaje(f.total(), totalGeneral)));
        }
        List<ReportCategoriesDemandedProjection> pagina = paginar(todas, limit, offset);
        return new Pagina<>(pagina, todas.size());
    }

    // ── Utilidades ───────────────────────────────────────────────────────

    private Predicate ilike(CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> campo, String texto) {
        return cb.like(cb.lower(campo), "%" + texto.toLowerCase(Locale.ROOT) + "%");
    }

    private BigDecimal porcentaje(long parte, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(parte * 100L)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private String textoAgregado(TreeSet<String> valores, String defecto) {
        if (valores == null || valores.isEmpty()) {
            return defecto;
        }
        return String.join(", ", valores);
    }

    private <T> List<T> limitar(List<T> filas, Integer maxLimit, boolean nuloEsTodo) {
        if (maxLimit == null) {
            return nuloEsTodo ? filas : filas.subList(0, Math.min(10, filas.size()));
        }
        return filas.subList(0, Math.min(maxLimit, filas.size()));
    }

    private <T> List<T> paginar(List<T> filas, int limit, int offset) {
        int desde = Math.min(offset, filas.size());
        int hasta = Math.min(desde + limit, filas.size());
        return filas.subList(desde, hasta);
    }

    private <T> T proyectar(Class<T> tipo, Object... pares) {
        Map<String, Object> valores = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            valores.put((String) pares[i], pares[i + 1]);
        }
        return PROYECCIONES.createProjection(tipo, valores);
    }
}
