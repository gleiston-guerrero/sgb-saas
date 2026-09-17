package com.uteq.backend.repository;

import com.uteq.backend.entity.Fine;
import com.uteq.backend.entity.StatusFine;
import com.uteq.backend.repository.projection.RecentPaymentProjection;
import com.uteq.backend.repository.projection.SummaryFinancialFinesProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
class FineProcedureRepositoryCustomImpl implements FineProcedureRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * Pays a fine through the stored procedure proc_pagar_multa (CREATE
     * PROCEDURE nativo de V51, invocado con CALL), que envuelve la función
     * sp_pagar_multa -- settles the balance and decides whether the lector
     * is unblocked. Binding exclusivamente posicional: ver nota extensa en
     * {@link LoanProcedureRepositoryCustomImpl#spCreateLoanProcedure} sobre
     * el conflicto Hibernate 6 / pgjdbc con parámetros nombrados. Sin SQL
     * nativo: P5.
     *
     * @param fineId identifier of the fine to pay in full
     * @return map with o_multa_id (paid fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spPayFineProcedure(Long fineId) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_pagar_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(3, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(2));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(3));
        return result;
    }

    /**
     * Voids a fine through the stored procedure proc_anular_multa (CREATE
     * PROCEDURE nativo de V51, SECURITY DEFINER, invocado con CALL), que
     * envuelve sp_anular_multa -- recording the reason and the role that
     * authorized the void for audit purposes.
     *
     * @param fineId identifier of the fine to void
     * @param reason business reason for the void, persisted in the audit trail
     * @param roleExecutor role of the staff member authorizing the void
     * @return map with o_multa_id (voided fine id) and o_usuario_desbloqueado
     *         (whether the lector account was unblocked as a result)
     */
    @Override
    public Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_anular_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(4, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(5, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.setParameter(2, reason);
        sp.setParameter(3, roleExecutor);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(4));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(5));
        return result;
    }

    /**
     * Registra un pago parcial a través de proc_pago_parcial_multa
     * (wrapper V54 de sp_pago_parcial_multa, FUNCTION con 4 OUT).
     * Mismas claves de salida que el SELECT anterior
     * (o_multa_id/o_estado/o_saldo_restante/o_usuario_desbloqueado).
     *
     * @param fineId identificador de la multa a abonar
     * @param amountPaid monto del abono parcial
     * @return mapa con las 4 salidas del procedimiento
     */
    @Override
    public Map<String, Object> spPaymentParcialFine(Long fineId, BigDecimal amountPaid) {
        StoredProcedureQuery sp = em.createStoredProcedureQuery("proc_pago_parcial_multa");
        sp.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(2, BigDecimal.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter(3, Long.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(5, BigDecimal.class, ParameterMode.OUT);
        sp.registerStoredProcedureParameter(6, Boolean.class, ParameterMode.OUT);
        sp.setParameter(1, fineId);
        sp.setParameter(2, amountPaid);
        sp.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("o_multa_id", (Long) sp.getOutputParameterValue(3));
        result.put("o_estado", (String) sp.getOutputParameterValue(4));
        result.put("o_saldo_restante", (BigDecimal) sp.getOutputParameterValue(5));
        result.put("o_usuario_desbloqueado", (Boolean) sp.getOutputParameterValue(6));
        return result;
    }

    private static final SpelAwareProxyProjectionFactory PROYECCIONES =
            new SpelAwareProxyProjectionFactory();

    /**
     * Réplica de {@code fn_reporte_resumen_financiero_multas}: totales
     * recaudado (PAGADA) y pendiente (PENDIENTE) en el rango, siempre una
     * fila con ceros NUMERIC(12,2) cuando no hay multas.
     *
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @return fila única con ambos totales
     */
    @Override
    public SummaryFinancialFinesProjection fnReportSummaryFinancial(
            OffsetDateTime from, OffsetDateTime until) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BigDecimal[]> query = cb.createQuery(BigDecimal[].class);
        Root<Fine> multa = query.from(Fine.class);
        Root<StatusFine> estado = query.from(StatusFine.class);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(estado.get("id"), multa.get("statusFineId")));
        if (from != null) {
            filtros.add(cb.greaterThanOrEqualTo(multa.get("dateGenerated"), from));
        }
        if (until != null) {
            filtros.add(cb.lessThanOrEqualTo(multa.get("dateGenerated"), until));
        }
        query.multiselect(
                cb.sum(cb.<BigDecimal>selectCase()
                        .when(cb.equal(estado.get("name"), "PAGADA"), multa.get("amount"))
                        .otherwise(BigDecimal.ZERO)),
                cb.sum(cb.<BigDecimal>selectCase()
                        .when(cb.equal(estado.get("name"), "PENDIENTE"), multa.get("amount"))
                        .otherwise(BigDecimal.ZERO)));
        query.where(filtros.toArray(Predicate[]::new));
        BigDecimal[] fila = em.createQuery(query).getSingleResult();
        Map<String, Object> valores = new HashMap<>();
        valores.put("totalRecaudado", fila[0] != null ? fila[0] : new BigDecimal("0.00"));
        valores.put("totalPending", fila[1] != null ? fila[1] : new BigDecimal("0.00"));
        return PROYECCIONES.createProjection(SummaryFinancialFinesProjection.class, valores);
    }

    /**
     * Réplica de {@code fn_pagos_recientes}: últimas multas PAGADAs con
     * lector y libro, ordenadas por fecha de pago descendente.
     *
     * @param limit tope de filas, nulo = 5
     * @return lista de pagos recientes
     */
    @Override
    public List<RecentPaymentProjection> fnPaymentsRecientes(Integer limit) {
        int tope = (limit != null) ? limit : 5;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Fine> multa = query.from(Fine.class);
        Root<com.uteq.backend.entity.Loan> prestamo = query.from(com.uteq.backend.entity.Loan.class);
        Root<com.uteq.backend.entity.User> usuario = query.from(com.uteq.backend.entity.User.class);
        Root<com.uteq.backend.entity.Book> libro = query.from(com.uteq.backend.entity.Book.class);
        Root<StatusFine> estado = query.from(StatusFine.class);
        query.multiselect(
                multa.get("id"), multa.get("amountPaid"), multa.get("datePaid"),
                usuario.get("email"),
                cb.concat(cb.concat(usuario.get("name"), " "), usuario.get("lastName")),
                libro.get("title"));
        query.where(
                cb.equal(prestamo.get("id"), multa.get("loanId")),
                cb.equal(usuario.get("id"), prestamo.get("userId")),
                cb.equal(libro.get("id"), prestamo.get("bookId")),
                cb.equal(estado.get("id"), multa.get("statusFineId")),
                cb.equal(estado.get("name"), "PAGADA"));
        query.orderBy(cb.desc(multa.get("datePaid")));
        TypedQuery<Object[]> consulta = em.createQuery(query);
        consulta.setMaxResults(tope);
        List<RecentPaymentProjection> resultado = new ArrayList<>();
        for (Object[] fila : consulta.getResultList()) {
            Map<String, Object> valores = new HashMap<>();
            valores.put("fineId", fila[0]);
            valores.put("amountPaid", fila[1]);
            valores.put("datePaid", fila[2] != null
                    ? ((OffsetDateTime) fila[2]).toInstant() : null);
            valores.put("userEmail", fila[3]);
            valores.put("userName", fila[4]);
            valores.put("bookTitle", fila[5]);
            resultado.add(PROYECCIONES.createProjection(RecentPaymentProjection.class, valores));
        }
        return resultado;
    }
}
