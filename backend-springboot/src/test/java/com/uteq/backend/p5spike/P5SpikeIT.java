package com.uteq.backend.p5spike;

import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.Loan;
import com.uteq.backend.entity.Reservation;
import com.uteq.backend.entity.AuditLogAudit;
import com.uteq.backend.repository.AuditLogAuditRepository;
import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.FineProcedureRepository;
import com.uteq.backend.repository.LoanProcedureRepository;
import com.uteq.backend.repository.LoanRepository;
import com.uteq.backend.repository.ReservationProcedureRepository;
import com.uteq.backend.repository.LoanRepository;
import com.uteq.backend.repository.ReservationRepository;
import com.uteq.backend.repository.StatusReservationRepository;
import com.uteq.backend.service.LoanService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spikes P5 contra PostgreSQL real (local sgb_db migrada a V53+R__).
 *
 * <p>Solo corre con {@code SGB_SPIKE_PG=1} y datasource PG por propiedades;
 * en CI queda deshabilitado. Prueba los mecanismos de reemplazo ANTES de
 * migrar las 33 consultas: si un spike falla, su grupo no se migra.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SGB_SPIKE_PG", matches = "1")
class P5SpikeIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private SpikeBookRepo books;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private StatusReservationRepository statuses;

    @Autowired
    private LoanRepository loans;

    @Autowired
    private BookRepository booksRepo;

    @Autowired
    private AuditLogAuditRepository auditoria;

    @Autowired
    private LoanProcedureRepository loanProcRepo;

    @Autowired
    private FineProcedureRepository fineProcRepo;

    @Autowired
    private ReservationProcedureRepository reservationProcRepo;

    @Test
    void s1_procedurePosicionalCreaPrestamo() {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_crear_prestamo");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(4, Integer.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(5, Long.class, ParameterMode.OUT);
        sp.setParameter(1, 2L);
        sp.setParameter(2, 1L);
        sp.setParameter(3, 1L);
        sp.setParameter(4, 14);
        sp.execute();

        Long id = (Long) sp.getOutputParameterValue(5);
        assertThat(id).isNotNull();
        Loan creado = em.find(Loan.class, id);
        assertThat(creado).isNotNull();
        assertThat(creado.getUserId()).isEqualTo(2L);
        assertThat(creado.getBookId()).isEqualTo(1L);
    }

    @Test
    void s3a_functionSimilarityEnJpql() {
        List<Book> top = books.suggest("clean cod", "ACTIVO", PageRequest.of(0, 10));
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).getTitle()).containsIgnoringCase("clean");
        assertThat(top.size()).isLessThanOrEqualTo(10);
    }

    @Test
    void s5_ventanaHoyYProximas() {
        Integer pend = statuses.findByName("PENDIENTE").orElseThrow().getId();
        OffsetDateTime startToday = LocalDate.now(java.time.ZoneId.systemDefault())
                .atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        sembrarReserva(2L, 1L, pend, startToday.plusHours(10));
        sembrarReserva(2L, 2L, pend, startToday.plusDays(2));
        sembrarReserva(2L, 1L, pend, startToday.minusDays(1));

        var hoy = reservations.searchReservationsToday(startToday, startToday.plusDays(1));
        assertThat(hoy).hasSize(1);
        assertThat(hoy.get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(hoy.get(0).getStatusName()).isEqualTo("PENDIENTE");
        assertThat(hoy.get(0).getDateLimitPickup()).isNotNull();

        var proximas = reservations.searchReservationsNexts(startToday.plusDays(1));
        assertThat(proximas).hasSize(1);
    }

    @Test
    void s6_activosPorUsuarioJpqlYDiasJava() {
        OffsetDateTime estimada = LocalDate.now(ZoneOffset.UTC)
                .plusDays(5).atStartOfDay().atOffset(ZoneOffset.UTC);
        Loan prestamo = new Loan();
        prestamo.setUserId(2L);
        prestamo.setBookId(1L);
        prestamo.setLibrarianId(1L);
        prestamo.setDateLoan(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        prestamo.setDateLoanReturnEstimada(estimada);
        prestamo.setStatusLoanId(1);
        prestamo = loans.saveAndFlush(prestamo);

        var filas = loans.findActivesByUserId(2L);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(filas.get(0).getStatusName()).isEqualTo("ACTIVO");

        Object diasSql = em.createNativeQuery(
                "SELECT (fecha_devolucion_estimada::date - NOW()::date) FROM prestamos WHERE id = ?")
                .setParameter(1, filas.get(0).getLoanId())
                .getSingleResult();
        assertThat(LoanService.diasRestantes(filas.get(0).getDateLoanReturnEstimada().toInstant()))
                .isEqualTo(((Number) diasSql).intValue());
    }

    @Test
    void s7_catalogoCriteriaEquivaleANativas() {
        var sinPaginar = org.springframework.data.domain.Pageable.unpaged();

        var soloTexto = booksRepo.searchText("clean", 1, null, null, null, sinPaginar);
        assertThat(soloTexto.getContent()).hasSize(1);
        assertThat(soloTexto.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(soloTexto.getTotalElements()).isEqualTo(1);

        var conCategoriaYStock = booksRepo.searchText("refactoring", 1, 2, null, true, sinPaginar);
        assertThat(conCategoriaYStock.getContent()).hasSize(1);

        var conAutor = booksRepo.searchText("cien", 1, null, 3L, null, sinPaginar);
        assertThat(conAutor.getContent()).hasSize(1);
        assertThat(conAutor.getContent().get(0).getTitle()).contains("Cien");

        var agotados = booksRepo.searchText("a", 1, null, null, false, sinPaginar);
        assertThat(agotados.getContent()).isEmpty();

        // Join a categorias sin duplicar el total (countDistinct).
        var porCategoria = booksRepo.searchText("a", 1, 1, null, null, sinPaginar);
        assertThat(porCategoria.getContent()).hasSize(2);
        assertThat(porCategoria.getTotalElements()).isEqualTo(2);

        var bandeja = booksRepo.searchByStatusesCriteria(java.util.List.of(1), "sapiens", null, sinPaginar);
        assertThat(bandeja.getContent()).hasSize(1);

        var bandejaSinFiltros = booksRepo.searchByStatusesCriteria(java.util.List.of(1), null, null, sinPaginar);
        assertThat(bandejaSinFiltros.getTotalElements()).isEqualTo(5);

        var sugerencias = booksRepo.suggestByTitleCriteria("clean", 1);
        assertThat(sugerencias).isNotEmpty();
        assertThat(sugerencias.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void s8_bitacoraCriteriaFiltrosYOrden() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        sembrarBitacora(1L, "UPDATE", "s8usuarios", "x", ahora.minusDays(9));
        sembrarBitacora(null, "LOGIN_FAIL", "s8sesiones", "y", ahora.minusHours(2));
        sembrarBitacora(1L, "INSERT", "s8prestamos", "z", ahora.minusHours(1));
        var orden = org.springframework.data.domain.Sort.by("fecha_hora").descending();
        var paginado = org.springframework.data.domain.PageRequest.of(0, 20, orden);

        var todo = auditoria.searchWithFiltersCriteria(null, null, null, null,
                org.springframework.data.domain.Pageable.unpaged());
        assertThat(todo.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(todo.getContent().get(0).getDateTime())
                .isAfterOrEqualTo(todo.getContent().get(1).getDateTime());

        var porUsuarioYModulo = auditoria.searchWithFiltersCriteria(1L, "s8prestamos", null, null, paginado);
        assertThat(porUsuarioYModulo.getContent()).hasSize(1);

        var porModulo = auditoria.searchWithFiltersCriteria(null, "s8sesiones", null, null, paginado);
        assertThat(porModulo.getContent()).hasSize(1);

        OffsetDateTime desde = ahora.minusDays(9).minusHours(1);
        OffsetDateTime hasta = ahora.minusDays(9).plusHours(1);
        var ventana = auditoria.searchWithFiltersCriteria(null, null, desde, hasta, paginado);
        assertThat(ventana.getContent())
                .allMatch(e -> !e.getDateTime().isBefore(desde) && !e.getDateTime().isAfter(hasta));
        assertThat(ventana.getContent().stream().map(AuditLogAudit::getDetalles)).contains("x");
    }

    private void sembrarBitacora(Long userId, String tipo, String modulo,
            String detalles, OffsetDateTime cuando) {        AuditLogAudit e = AuditLogAudit.builder()
                .userId(userId)
                .typeOperacion(tipo)
                .tableAfectada(modulo)
                .detalles(detalles)
                .dateTime(cuando)
                .build();
        auditoria.save(e);
    }

    private void sembrarReserva(Long userId, Long bookId, Integer statusId, OffsetDateTime limite) {
        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setStatusReservationId(statusId);
        r.setDateReservation(limite.minusDays(3));
        r.setDateLimitPickup(limite);
        reservations.save(r);
    }

    @Test
    void s9a_devolucionSinAtraso_noGeneraMulta() {
        Long loanId = loanProcRepo.spCreateLoanProcedure(2L, 1L, 1L, 7);
        var result = loanProcRepo.spRegisterLoanReturn(loanId);
        assertThat(result.get("o_prestamo_id")).isEqualTo(loanId);
        assertThat(result.get("o_hubo_multa")).isEqualTo(false);
        assertThat(result.get("o_monto_multa")).isNull();
    }

    @Test
    void s9b_pagoTotal_desbloquea() {
        Long fineId = sembrarMultaPendiente();
        var result = fineProcRepo.spPayFineProcedure(fineId);
        assertThat(result.get("o_multa_id")).isEqualTo(fineId);
        assertThat(result.get("o_usuario_desbloqueado")).isEqualTo(true);
    }

    @Test
    void s9c_anulacionGerente_desbloquea() {
        Long fineId = sembrarMultaPendiente();
        var result = fineProcRepo.spVoidFineProcedure(fineId, "spike P5", "GERENTE");
        assertThat(result.get("o_multa_id")).isEqualTo(fineId);
        assertThat(result.get("o_usuario_desbloqueado")).isEqualTo(true);
    }

    @Test
    void s9d_expirarVencidas_cuentaExpiradas() {
        Integer pend = statuses.findByName("PENDIENTE").orElseThrow().getId();
        OffsetDateTime ayer = LocalDate.now(java.time.ZoneId.systemDefault())
                .minusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        sembrarReserva(2L, 2L, pend, ayer);
        Integer expiradas = reservationProcRepo.spExpireReservationsVencidasProcedure();
        assertThat(expiradas).isGreaterThanOrEqualTo(1);
    }

    @Test
    void s9e_pagoParcial_mantienePendienteConSaldo() {
        Long fineId = sembrarMultaPendiente();
        java.math.BigDecimal total = (java.math.BigDecimal) em
                .createNativeQuery("SELECT monto FROM multas WHERE id = ?")
                .setParameter(1, fineId)
                .getSingleResult();
        java.math.BigDecimal abono = total.divide(new java.math.BigDecimal("2"));
        var result = fineProcRepo.spPaymentParcialFine(fineId, abono);
        assertThat(result.get("o_multa_id")).isEqualTo(fineId);
        assertThat(result.get("o_estado")).isEqualTo("PENDIENTE");
        assertThat((java.math.BigDecimal) result.get("o_saldo_restante"))
                .isGreaterThan(java.math.BigDecimal.ZERO);
    }

    private Long sembrarMultaPendiente() {        Long loanId = loanProcRepo.spCreateLoanProcedure(2L, 1L, 1L, 7);
        em.createNativeQuery("UPDATE prestamos SET fecha_devolucion_estimada = NOW() - INTERVAL '2 days' WHERE id = ?")
                .setParameter(1, loanId)
                .executeUpdate();
        em.flush();
        em.clear();
        loanProcRepo.spRegisterLoanReturn(loanId);
        return (Long) em.createNativeQuery("SELECT id FROM multas WHERE prestamo_id = ?")
                .setParameter(1, loanId)
                .getSingleResult();
    }

    @Test
    @SuppressWarnings("unchecked")
    void s10_resumenYPagos_equivalenAFuncion() {
        Long pagadaId = sembrarMultaPendiente();
        fineProcRepo.spPayFineProcedure(pagadaId);
        sembrarMultaPendiente();

        var resumen = fineProcRepo.fnReportSummaryFinancial(null, null);
        var filaFn = (Object[]) em.createNativeQuery(
                "SELECT * FROM fn_reporte_resumen_financiero_multas(NULL, NULL)")
                .getSingleResult();
        assertThat(resumen.getTotalRecaudado()).isEqualTo(filaFn[0]);
        assertThat(resumen.getTotalPending()).isEqualTo(filaFn[1]);

        var pagos = fineProcRepo.fnPaymentsRecientes(5);
        var filasFn = em.createNativeQuery("SELECT * FROM fn_pagos_recientes(5)").getResultList();
        assertThat(pagos).hasSize(filasFn.size()).isNotEmpty();
        for (int i = 0; i < pagos.size(); i++) {
            Object[] f = (Object[]) filasFn.get(i);
            assertThat(pagos.get(i).getFineId()).isEqualTo(((Number) f[0]).longValue());
            assertThat(pagos.get(i).getAmountPaid()).isEqualTo(f[1]);
            assertThat(pagos.get(i).getUserEmail()).isEqualTo(f[3]);
            assertThat(pagos.get(i).getUserName()).isEqualTo(f[4]);
            assertThat(pagos.get(i).getBookTitle()).isEqualTo(f[5]);
        }
    }
}
