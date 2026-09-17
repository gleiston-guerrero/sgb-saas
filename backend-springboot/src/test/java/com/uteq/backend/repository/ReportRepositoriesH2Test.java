package com.uteq.backend.repository;

import com.uteq.backend.entity.AuditLogAudit;
import com.uteq.backend.entity.Author;
import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.Category;
import com.uteq.backend.entity.ConfigurationSystem;
import com.uteq.backend.entity.Fine;
import com.uteq.backend.entity.Language;
import com.uteq.backend.entity.Loan;
import com.uteq.backend.entity.Publisher;
import com.uteq.backend.entity.Reservation;
import com.uteq.backend.entity.StatusBook;
import com.uteq.backend.entity.StatusFine;
import com.uteq.backend.entity.StatusLoan;
import com.uteq.backend.entity.StatusReservation;
import com.uteq.backend.entity.StatusUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura H2 de los repositorios Criteria/JPQL de P5 (sin Docker).
 * Semillas mínimas por test (rollback); la equivalencia exacta contra
 * PostgreSQL vive en P5SpikeIT/P5TabularSpikeIT.
 */
@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
@Sql(statements = {
        "CREATE ALIAS IF NOT EXISTS similarity FOR \"com.uteq.backend.repository.H2Funciones.similarity\""
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ReportRepositoriesH2Test {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BookRepository libros;
    @Autowired
    private LoanRepository prestamos;
    @Autowired
    private LoanProcedureRepository reportes;
    @Autowired
    private FineProcedureRepository reportesMultas;
    @Autowired
    private AuditLogAuditRepository bitacora;
    @Autowired
    private ReservationRepository reservaciones;
    @Autowired
    private StatusReservationRepository estadosReservacion;
    @Autowired
    private ConfigurationSystemRepository configuracion;

    private Publisher editorial;
    private Language idioma;
    private StatusBook estadoLibro;
    private Category cat1;
    private Category cat2;
    private Author autor1;
    private Long lectorId;
    private StatusLoan actLoan;
    private StatusLoan devLoan;
    private StatusFine pendFine;
    private StatusFine pagadaFine;

    @BeforeEach
    void sembrarBase() {
        editorial = guardar(new Publisher(), "name", "Ed Test");
        Language nuevoIdioma = new Language();
        nuevoIdioma.setCode("es");
        idioma = guardar(nuevoIdioma, "name", "Español");
        estadoLibro = guardar(new StatusBook(), "name", "ACTIVO");
        cat1 = guardar(new Category(), "name", "Ficción");
        cat2 = guardar(new Category(), "name", "Tecnología");
        autor1 = guardar(new Author(), "name", "Autora Test");
        actLoan = guardar(new StatusLoan(), "name", "ACTIVO");
        devLoan = guardar(new StatusLoan(), "name", "DEVUELTO");
        pendFine = guardar(new StatusFine(), "name", "PENDIENTE");
        pagadaFine = guardar(new StatusFine(), "name", "PAGADA");
        guardar(new StatusReservation(), "name", "PENDIENTE");
        guardar(new StatusReservation(), "name", "LISTA_PARA_RETIRO");
        var estadoUsuario = guardar(new StatusUser(), "name", "ACTIVO");
        // UUID con default PG (uuid_generate_v4) inexistente en H2: insert
        // nativo con token explícito (insertable=false lo excluye del ORM).
        em.createNativeQuery(
                "INSERT INTO usuarios (nombre, apellido, correo, password_hash,"
                        + " estado_id, correo_verificado, fecha_registro, actualizado_en,"
                        + " credencial_qr_token) VALUES"
                        + " ('Lector', 'Base', 'lector.base@correo.com', 'x',"
                        + " :estado, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,"
                        + " RANDOM_UUID())")
                .setParameter("estado", estadoUsuario.getId())
                .executeUpdate();
        lectorId = em.createQuery("SELECT u.id FROM User u WHERE u.email = :c", Long.class)
                .setParameter("c", "lector.base@correo.com")
                .getSingleResult();
        var cfg = new ConfigurationSystem();
        cfg.setKey("monto_multa_diaria");
        cfg.setValue("0.50");
        em.persist(cfg);
        em.flush();
    }

    @SuppressWarnings("unchecked")
    private <T> T guardar(T entidad, String campo, Object valor) {
        try {
            var f = entidad.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(entidad, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        em.persist(entidad);
        return entidad;
    }

    private Book sembrarLibro(String titulo, String isbn, short stock, Category cat) {
        Book libro = new Book();
        libro.setTitle(titulo);
        libro.setIsbn(isbn);
        libro.setPublisher(editorial);
        libro.setLanguage(idioma);
        libro.setStatus(estadoLibro);
        libro.setStockTotal(stock);
        libro.setStockAvailable(stock);
        libro.setYearPublication((short) 2020);
        if (cat != null) {
            libro.setCategories(new HashSet<>(List.of(cat)));
        }
        em.persist(libro);
        em.flush();
        return libro;
    }

    private Loan sembrarPrestamo(Long userId, Long bookId, int prestamoHaceDias,
            Integer devuelveEnDias, Integer estadoId, Integer devueltoHaceDias) {
        Loan prestamo = new Loan();
        prestamo.setUserId(userId);
        prestamo.setBookId(bookId);
        prestamo.setLibrarianId(userId);
        prestamo.setDateLoan(OffsetDateTime.now().minusDays(prestamoHaceDias));
        prestamo.setDateLoanReturnEstimada(OffsetDateTime.now().plusDays(devuelveEnDias));
        prestamo.setStatusLoanId(estadoId);
        em.persist(prestamo);
        em.flush();
        if (devueltoHaceDias != null) {
            prestamo.setDateLoanReturnReal(OffsetDateTime.now().minusDays(devueltoHaceDias));
        }
        return prestamo;
    }

    private Fine sembrarMulta(Long prestamoId, String monto, Integer estadoId, Integer generadaHaceDias) {
        Fine multa = new Fine();
        multa.setLoanId(prestamoId);
        multa.setAmount(new BigDecimal(monto));
        multa.setAmountPaid(BigDecimal.ZERO);
        multa.setStatusFineId(estadoId);
        multa.setDateGenerated(OffsetDateTime.now().minusDays(generadaHaceDias));
        em.persist(multa);
        em.flush();
        return multa;
    }

    // ── Book Criteria ──

    @Test
    void libro_textoCategoriaDisponibilidad() {
        sembrarLibro("Clean Code", "111", (short) 3, cat2);
        sembrarLibro("Refactoring", "222", (short) 0, cat2);
        var pagina = PageRequest.of(0, 10);
        assertThat(libros.searchText("clean", estadoLibro.getId(), null, null, null, pagina)
                .getContent()).hasSize(1);
        assertThat(libros.searchText("e", estadoLibro.getId(), cat2.getId(), null, true, pagina)
                .getTotalElements()).isEqualTo(1);
        assertThat(libros.searchText("e", estadoLibro.getId(), null, null, false, pagina)
                .getContent()).hasSize(1);
        assertThat(libros.searchText("clean", estadoLibro.getId(), null, autor1.getId(), null, pagina)
                .getContent()).isEmpty();
    }

    @Test
    void libro_estadosYAnio() {
        sembrarLibro("Clean Code", "111", (short) 3, cat1);
        var pagina = PageRequest.of(0, 10, Sort.by("title"));
        assertThat(libros.searchByStatusesCriteria(List.of(estadoLibro.getId()), null, null, pagina)
                .getTotalElements()).isEqualTo(1);
        assertThat(libros.searchByStatusesCriteria(List.of(estadoLibro.getId()), "", (short) 2020, pagina)
                .getContent()).hasSize(1);
        assertThat(libros.searchByStatusesCriteria(List.of(estadoLibro.getId()), "zzz", null, pagina)
                .getContent()).isEmpty();
        assertThat(libros.searchByStatusesCriteria(List.of(estadoLibro.getId()), null, (short) 1999, pagina)
                .getContent()).isEmpty();
    }

    @Test
    void libro_sugerencias() {
        sembrarLibro("Clean Code", "111", (short) 3, cat1);
        assertThat(libros.suggestByTitleCriteria("clean code", estadoLibro.getId())).isNotEmpty();
        assertThat(libros.suggestByTitleCriteria("zzzqqq", estadoLibro.getId())).isEmpty();
    }

    // ── Bitácora Criteria ──

    private void sembrarBitacora(Long userId, String tipo, String modulo, int haceDias) {
        AuditLogAudit e = AuditLogAudit.builder()
                .userId(userId).typeOperacion(tipo).tableAfectada(modulo)
                .detalles("d").dateTime(OffsetDateTime.now().minusDays(haceDias)).build();
        bitacora.save(e);
    }

    @Test
    void bitacora_filtrosYOrden() {
        sembrarBitacora(lectorId, "UPDATE", "usuarios", 9);
        sembrarBitacora(null, "LOGIN_FAIL", "sesiones", 0);
        var paginado = PageRequest.of(0, 20, Sort.by("fecha_hora").descending());
        assertThat(bitacora.searchWithFiltersCriteria(null, null, null, null, Pageable.unpaged())
                .getTotalElements()).isEqualTo(2);
        assertThat(bitacora.searchWithFiltersCriteria(lectorId, null, null, null, paginado)
                .getContent()).hasSize(1);
        assertThat(bitacora.searchWithFiltersCriteria(null, "sesiones", null, null, paginado)
                .getContent()).hasSize(1);
        var desde = OffsetDateTime.now().minusDays(1);
        assertThat(bitacora.searchWithFiltersCriteria(null, null, desde, null, paginado)
                .getContent()).hasSize(1);
        assertThat(bitacora.searchWithFiltersCriteria(null, null, null, desde, paginado)
                .getContent()).hasSize(1);
    }

    // ── Reservaciones/préstamos JPQL ──

    @Test
    void reservas_ventana() {
        Integer pend = estadosReservacion.findByName("PENDIENTE").orElseThrow().getId();
        OffsetDateTime inicio = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
        sembrarReserva(lectorId, sembrarLibro("Clean Code", "111", (short) 3, cat1).getId(),
                pend, inicio.plusHours(10));
        assertThat(reservaciones.searchReservationsToday(inicio, inicio.plusDays(1))).hasSize(1);
        assertThat(reservaciones.searchReservationsNexts(inicio.plusDays(1))).isEmpty();
    }

    private void sembrarReserva(Long userId, Long bookId, Integer statusId, OffsetDateTime limite) {
        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setStatusReservationId(statusId);
        r.setDateReservation(limite.minusDays(3));
        r.setDateLimitPickup(limite);
        reservaciones.save(r);
    }

    @Test
    void prestamos_activos() {
        Book libro = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        sembrarPrestamo(lectorId, libro.getId(), 2, 5, actLoan.getId(), null);
        var filas = prestamos.findActivesByUserId(lectorId);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getBookTitle()).isEqualTo("Clean Code");
    }

    // ── Reportes tabulares ──

    @Test
    void reportes_topYMorosidad() {
        Book b1 = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        sembrarPrestamo(lectorId, b1.getId(), 20, -2, actLoan.getId(), null);
        sembrarMulta(ultimoPrestamoId(), "10.00", pendFine.getId(), 1);
        assertThat(reportes.fnReportBooksMostLoaned(10, null, null)).hasSize(1);
        var morosos = reportes.fnReportIndexDelinquency(10);
        assertThat(morosos).hasSize(1);
        assertThat(morosos.get(0).getAmountTotalAdeudado())
                .isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(morosos.get(0).getQuantityFinesPendientes()).isEqualTo(1);
        assertThat(reportes.countReportIndexDelinquency(10)).isEqualTo(1);
        assertThat(reportes.fnReportIndexDelinquencyPaginated(10, 10, 0)).hasSize(1);
    }

    @Test
    void reportes_usoYCategorias() {
        Book b1 = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        sembrarPrestamo(lectorId, b1.getId(), 2, 5, actLoan.getId(), null);
        assertThat(reportes.fnReportUsageByPeriod("dia", null, null)).isNotEmpty();
        assertThat(reportes.fnReportUsageByPeriod("semana", null, null)).isNotEmpty();
        assertThat(reportes.fnReportUsageByPeriod("mes", null, null)).isNotEmpty();
        assertThat(reportes.countReportUsageByPeriod("dia", null, null)).isPositive();
        var cats = reportes.fnReportCategoriesDemanded(10, null, null);
        assertThat(cats).hasSize(1);
        assertThat(cats.get(0).getPercentage()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(reportes.countReportCategoriesDemanded(10, null, null)).isEqualTo(1);
    }

    @Test
    void reportes_detalladoEInventario() {
        Book b1 = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        b1.setAuthors(new HashSet<>(List.of(autor1)));
        sembrarPrestamo(lectorId, b1.getId(), 2, 5, actLoan.getId(), null);
        var det = reportes.fnReportBooksMostLoanedDetailed(10, null, null, null);
        assertThat(det).hasSize(1);
        assertThat(det.get(0).getAuthorName()).contains("Autora");
        assertThat(reportes.countReportBooksDetailed(10, null, null, null)).isEqualTo(1);
        var inv = reportes.fnReportInventory(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        assertThat(inv).hasSize(1);
        assertThat(inv.get(0).getStatusAvailability()).isEqualTo("Disponible");
        assertThat(reportes.countReportInventory(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null)).isEqualTo(1);
        var agotados = reportes.fnReportInventory(null, "agotado", null, null, null, null, null,
                null, null, null, null, null, null, null);
        assertThat(agotados).isEmpty();
    }

    @Test
    void reportes_vencidosYResumen() {
        Book b1 = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        sembrarPrestamo(lectorId, b1.getId(), 20, -2, actLoan.getId(), null);
        var vencidos = reportes.fnReportLoansOverdues(null, null);
        assertThat(vencidos).hasSize(1);
        assertThat(vencidos.get(0).getDaysAtraso()).isPositive();
        assertThat(vencidos.get(0).getAmountFineEstimada())
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(reportes.countReportLoansOverdues(null, null)).isEqualTo(1);
        sembrarMulta(ultimoPrestamoId(), "10.00", pendFine.getId(), 1);
        var resumen = reportesMultas.fnReportSummaryFinancial(null, null);
        assertThat(resumen.getTotalPending()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(resumen.getTotalRecaudado()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(reportesMultas.fnPaymentsRecientes(5)).isEmpty();
    }

    @Test
    void reportes_activosPorUsuario() {
        Book b1 = sembrarLibro("Clean Code", "111", (short) 3, cat1);
        sembrarPrestamo(lectorId, b1.getId(), 2, 5, actLoan.getId(), null);
        assertThat(reportes.fnListLoansActivesByUser(lectorId)).hasSize(1);
    }

    private Long ultimoPrestamoId() {
        return em.createQuery("SELECT p.id FROM Loan p ORDER BY p.id DESC", Long.class)
                .setMaxResults(1).getSingleResult();
    }
}
