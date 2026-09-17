package com.uteq.backend.repository;

import com.uteq.backend.entity.AuditLogAudit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
class AuditLogAuditRepositoryCustomImpl implements AuditLogAuditRepositoryCustom {

    /** Nombres físicos históricos aceptados y su propiedad de entidad. */
    private static final Map<String, String> SORT_LEGACY = Map.of("fecha_hora", "dateTime");

    /**
     * Filtra la bitácora por usuario, módulo y rango de fechas, paginado.
     *
     * @param userId autor del evento, nulo = todos
     * @param module tabla afectada, nulo = todas
     * @param from inicio del rango, nulo = sin inicio
     * @param until fin del rango, nulo = sin fin
     * @param pageable paginación y orden con propiedades de entidad
     * @return página de eventos coincidentes
     */
    @Override
    public Page<AuditLogAudit> searchWithFiltersCriteria(Long userId, String module,
            OffsetDateTime from, OffsetDateTime until, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<AuditLogAudit> query = cb.createQuery(AuditLogAudit.class);
        Root<AuditLogAudit> bitacora = query.from(AuditLogAudit.class);
        List<Predicate> filtros = filtros(cb, bitacora, userId, module, from, until);
        query.select(bitacora).where(filtros.toArray(Predicate[]::new));
        query.orderBy(ordenes(cb, bitacora, pageable.getSort()));

        TypedQuery<AuditLogAudit> contenido = em.createQuery(query);
        if (pageable.isPaged()) {
            contenido.setFirstResult((int) pageable.getOffset());
            contenido.setMaxResults(pageable.getPageSize());
        }
        List<AuditLogAudit> filas = contenido.getResultList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<AuditLogAudit> base = conteo.from(AuditLogAudit.class);
        conteo.select(cb.count(base))
                .where(filtros(cb, base, userId, module, from, until).toArray(Predicate[]::new));
        long total = em.createQuery(conteo).getSingleResult();
        return new PageImpl<>(filas, pageable, total);
    }

    @PersistenceContext
    private EntityManager em;

    private List<Predicate> filtros(CriteriaBuilder cb, Root<AuditLogAudit> bitacora,
            Long userId, String module, OffsetDateTime from, OffsetDateTime until) {
        List<Predicate> filtros = new ArrayList<>();
        if (userId != null) {
            filtros.add(cb.equal(bitacora.get("userId"), userId));
        }
        if (module != null) {
            filtros.add(cb.equal(bitacora.get("tableAfectada"), module));
        }
        if (from != null) {
            filtros.add(cb.greaterThanOrEqualTo(bitacora.get("dateTime"), from));
        }
        if (until != null) {
            filtros.add(cb.lessThanOrEqualTo(bitacora.get("dateTime"), until));
        }
        return filtros;
    }

    private List<Order> ordenes(CriteriaBuilder cb, Root<AuditLogAudit> bitacora, Sort sort) {
        List<Order> resultado = new ArrayList<>();
        for (Sort.Order orden : sort) {
            String propiedad = SORT_LEGACY.getOrDefault(orden.getProperty(), orden.getProperty());
            var camino = bitacora.get(propiedad);
            resultado.add(orden.isAscending() ? cb.asc(camino) : cb.desc(camino));
        }
        if (resultado.isEmpty()) {
            resultado.add(cb.desc(bitacora.get("dateTime")));
        }
        return resultado;
    }
}
