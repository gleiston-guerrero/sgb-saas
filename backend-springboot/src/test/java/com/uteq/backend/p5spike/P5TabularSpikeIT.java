package com.uteq.backend.p5spike;

import com.uteq.backend.repository.LoanProcedureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalencia fila a fila entre los reportes reimplementados (P5) y las
 * funciones fn_* sobre los mismos datos. Solo corre con
 * {@code SGB_SPIKE_PG=1} contra PostgreSQL real (rollback por test).
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SGB_SPIKE_PG", matches = "1")
class P5TabularSpikeIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private LoanProcedureRepository repo;

    private OffsetDateTime dias(int d) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(d);
    }

    /** Semilla determinista: 4 préstamos + 3 multas (rollback al terminar). */
    private void sembrar() {
        // L1 user2/book1 hace 10d, estimada +5d, ACTIVO(1)
        // L2 user2/book2 hace 20d, estimada -2d, ACTIVO(1) (vencido)
        // L3 user2/book3 hace 40d, real hace 5d, DEVUELTO(3)
        // L4 user2/book4 hace 3d, estimada +11d, RENOVADO(2)
        // L5 user2/book1 hace 1d, estimada +13d, ACTIVO(1)
        em.createNativeQuery("INSERT INTO prestamos "
                + "(usuario_id, libro_id, bibliotecario_id, fecha_prestamo,"
                + " fecha_devolucion_estimada, estado_prestamo_id) VALUES "
                + "(2, 1, 1, ?1, ?2, 1), (2, 2, 1, ?3, ?4, 1),"
                + " (2, 3, 1, ?5, ?6, 3), (2, 4, 1, ?7, ?8, 2), (2, 1, 1, ?9, ?10, 1)")
                .setParameter(1, dias(-10)).setParameter(2, dias(5))
                .setParameter(3, dias(-20)).setParameter(4, dias(-2))
                .setParameter(5, dias(-40)).setParameter(6, dias(-30))
                .setParameter(7, dias(-3)).setParameter(8, dias(11))
                .setParameter(9, dias(-1)).setParameter(10, dias(13))
                .executeUpdate();
        em.createNativeQuery("UPDATE prestamos SET fecha_devolucion_real = ?1"
                + " WHERE libro_id = 3 AND usuario_id = 2")
                .setParameter(1, dias(-5)).executeUpdate();
        // F1 PENDIENTE(1) sobre L2 (libro 2): 10.00, generada ayer
        // F2 PENDIENTE(1) sobre L4 (libro 4): 4.00, generada hoy
        // F3 PAGADA(2) sobre L3 (libro 3): 6.00, pagada hoy
        em.createNativeQuery("INSERT INTO multas (prestamo_id, monto, estado_multa_id,"
                + " fecha_generada) SELECT p.id, 10.00, 1, ?1 FROM prestamos p"
                + " WHERE p.libro_id = 2 AND p.usuario_id = 2")
                .setParameter(1, dias(-1)).executeUpdate();
        em.createNativeQuery("INSERT INTO multas (prestamo_id, monto, estado_multa_id,"
                + " fecha_generada) SELECT p.id, 4.00, 1, ?1 FROM prestamos p"
                + " WHERE p.libro_id = 4 AND p.usuario_id = 2")
                .setParameter(1, dias(0)).executeUpdate();
        em.createNativeQuery("INSERT INTO multas (prestamo_id, monto, monto_pagado,"
                + " estado_multa_id, fecha_generada, fecha_pagada)"
                + " SELECT p.id, 6.00, 6.00, 2, ?1, ?2 FROM prestamos p"
                + " WHERE p.libro_id = 3 AND p.usuario_id = 2")
                .setParameter(1, dias(-6)).setParameter(2, dias(0)).executeUpdate();
        em.flush();
    }

    @Test
    @SuppressWarnings("unchecked")
    void t1_activos() {
        sembrar();
        var nuevos = repo.fnListLoansActivesByUser(2L);
        var viejos = (List<Object[]>) em
                .createNativeQuery("SELECT * FROM fn_listar_prestamos_activos_por_usuario(2)")
                .getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        for (int i = 0; i < nuevos.size(); i++) {
            assertThat(nuevos.get(i).getLoanId()).isEqualTo(((Number) viejos.get(i)[0]).longValue());
            assertThat(nuevos.get(i).getBookTitle()).isEqualTo(viejos.get(i)[1]);
            assertThat(nuevos.get(i).getStatusName()).isEqualTo(viejos.get(i)[6]);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void t2_topLibros() {
        sembrar();
        var nuevos = repo.fnReportBooksMostLoaned(10, null, null);
        var viejos = (List<Object[]>) em
                .createNativeQuery("SELECT * FROM fn_reporte_libros_mas_prestados(10, NULL, NULL)")
                .getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        assertThat(nuevos.get(0).getBookId()).isEqualTo(1L);
        assertThat(nuevos.get(0).getTotalLoans()).isEqualTo(2L);
        assertThat(nuevos.stream().map(p -> p.getBookId() + ":" + p.getTotalLoans()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(viejos.stream().map(f -> ((Number) f[0]).longValue() + ":" + ((Number) f[3]).longValue())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void t3_morosidad() {
        sembrar();
        var nuevos = repo.fnReportIndexDelinquency(10);
        var viejos = (List<Object[]>) em
                .createNativeQuery("SELECT * FROM fn_reporte_indice_morosidad(10)")
                .getResultList();
        assertThat(nuevos).hasSize(1);
        assertThat(viejos).hasSize(1);
        var n = nuevos.get(0);
        Object[] v = viejos.get(0);
        assertThat(n.getUserId()).isEqualTo(((Number) v[0]).longValue());
        assertThat(n.getAmountTotalAdeudado()).isEqualByComparingTo((BigDecimal) v[4]);
        assertThat(n.getQuantityFinesPendientes()).isEqualTo(((Number) v[5]).longValue());
        assertThat(n.getDaysAtrasoPromedio()).isEqualByComparingTo((BigDecimal) v[6]);
        assertThat(repo.countReportIndexDelinquency(10)).isEqualTo(1);
        assertThat(repo.fnReportIndexDelinquencyPaginated(10, 10, 0)).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void t4_usoPorPeriodo() {
        sembrar();
        var nuevos = repo.fnReportUsageByPeriod("dia", null, null);
        var viejos = (List<Object[]>) em
                .createNativeQuery("SELECT * FROM fn_reporte_uso_por_periodo('dia', NULL, NULL)")
                .getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        long prestamosNuevos = nuevos.stream().mapToLong(p -> p.getTotalLoans()).sum();
        assertThat(prestamosNuevos).isEqualTo(5);
        long devNuevas = nuevos.stream().mapToLong(p -> p.getTotalLoanReturns()).sum();
        assertThat(devNuevas).isEqualTo(1);
        assertThat(repo.countReportUsageByPeriod("dia", null, null)).isEqualTo(viejos.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void t5_detallado() {
        sembrar();
        var nuevos = repo.fnReportBooksMostLoanedDetailed(10, null, null, null);
        var viejos = (List<Object[]>) em.createNativeQuery(
                "SELECT * FROM fn_reporte_libros_mas_prestados_detallado(10, NULL, NULL, NULL)")
                .getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        assertThat(nuevos.get(0).getBookId()).isEqualTo(1L);
        assertThat(nuevos.stream().map(p -> p.getBookId() + ":" + p.getTotalLoans() + ":" + p.getPercentage()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(viejos.stream().map(f -> ((Number) f[0]).longValue() + ":" + ((Number) f[5]).longValue() + ":" + f[6])
                        .collect(java.util.stream.Collectors.toSet()));
        assertThat(repo.countReportBooksDetailed(10, null, null, null)).isEqualTo(viejos.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void t6_inventario() {
        sembrar();
        var nuevos = repo.fnReportInventory(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        var viejos = (List<Object[]>) em.createNativeQuery(
                "SELECT * FROM fn_reporte_inventario(NULL, NULL, NULL, NULL, NULL, NULL,"
                        + " NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)")
                .getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        for (int i = 0; i < nuevos.size(); i++) {
            assertThat(nuevos.get(i).getBookId()).isEqualTo(((Number) viejos.get(i)[0]).longValue());
            assertThat(nuevos.get(i).getTitle()).isEqualTo(viejos.get(i)[1]);
            assertThat(nuevos.get(i).getStatusAvailability()).isEqualTo(viejos.get(i)[13]);
        }
        var filtrados = repo.fnReportInventory(2, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        assertThat(filtrados).hasSize(2);
        assertThat(repo.countReportInventory(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null)).isEqualTo(viejos.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void t7_vencidos() {
        sembrar();
        var nuevos = repo.fnReportLoansOverdues(null, null);
        var viejos = (List<Object[]>) em
                .createNativeQuery("SELECT * FROM fn_reporte_prestamos_vencidos(NULL, NULL, NULL)")
                .getResultList();
        assertThat(nuevos).hasSize(1);
        assertThat(viejos).hasSize(1);
        Object[] v = viejos.get(0);
        assertThat(nuevos.get(0).getLoanId()).isEqualTo(((Number) v[0]).longValue());
        assertThat(nuevos.get(0).getDaysAtraso()).isEqualTo(((Number) v[6]).longValue());
        assertThat(nuevos.get(0).getAmountFineEstimada()).isEqualByComparingTo((BigDecimal) v[7]);
        assertThat(repo.countReportLoansOverdues(null, null)).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void t8_categorias() {
        sembrar();
        var nuevos = repo.fnReportCategoriesDemanded(10, null, null);
        var viejos = (List<Object[]>) em.createNativeQuery(
                "SELECT * FROM fn_reporte_categorias_demandadas(10, NULL, NULL)").getResultList();
        assertThat(nuevos).hasSize(viejos.size());
        assertThat(nuevos.stream().map(p -> p.getCategoryId() + ":" + p.getTotalLoans() + ":" + p.getPercentage()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(viejos.stream().map(f -> ((Number) f[0]).intValue() + ":" + ((Number) f[2]).longValue() + ":" + f[3])
                        .collect(java.util.stream.Collectors.toSet()));
    }
}
