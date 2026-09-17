package com.uteq.backend.p5spike;

import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.Loan;
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
}
