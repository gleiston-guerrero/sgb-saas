package com.uteq.backend.repository;

import com.uteq.backend.entity.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
class BookRepositoryCustomImpl implements BookRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Book> searchText(String q, Integer statusId, Integer categoryId,
            Long authorId, Boolean available, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(book.get("status").get("id"), statusId));
        filtros.add(textoPredicate(cb, book, q));
        if (categoryId != null) {
            filtros.add(cb.equal(book.join("categories", JoinType.INNER).get("id"), categoryId));
        }
        if (authorId != null) {
            filtros.add(cb.equal(book.join("authors", JoinType.INNER).get("id"), authorId));
        }
        if (available != null) {
            filtros.add(available
                    ? cb.greaterThan(book.get("stockAvailable"), (short) 0)
                    : cb.equal(book.get("stockAvailable"), (short) 0));
        }
        query.select(book).distinct(true).where(filtros.toArray(Predicate[]::new));
        query.orderBy(ordenes(cb, query, book, pageable.getSort()));

        TypedQuery<Book> contenido = em.createQuery(query);
        if (pageable.isPaged()) {
            contenido.setFirstResult((int) pageable.getOffset());
            contenido.setMaxResults(pageable.getPageSize());
        }
        List<Book> filas = contenido.getResultList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Book> base = conteo.from(Book.class);
        List<Predicate> filtrosConteo = new ArrayList<>();
        filtrosConteo.add(cb.equal(base.get("status").get("id"), statusId));
        filtrosConteo.add(textoPredicate(cb, base, q));
        if (categoryId != null) {
            filtrosConteo.add(cb.equal(base.join("categories", JoinType.INNER).get("id"), categoryId));
        }
        if (authorId != null) {
            filtrosConteo.add(cb.equal(base.join("authors", JoinType.INNER).get("id"), authorId));
        }
        if (available != null) {
            filtrosConteo.add(available
                    ? cb.greaterThan(base.get("stockAvailable"), (short) 0)
                    : cb.equal(base.get("stockAvailable"), (short) 0));
        }
        conteo.select(cb.countDistinct(base)).where(filtrosConteo.toArray(Predicate[]::new));
        long total = em.createQuery(conteo).getSingleResult();
        return new PageImpl<>(filas, pageable, total);
    }

    @Override
    public Page<Book> searchByStatusesCriteria(List<Integer> statusIds, String q,
            Short year, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(book.get("status").get("id").in(statusIds));
        if (q != null && !q.isBlank()) {
            filtros.add(textoPredicate(cb, book, q));
        }
        if (year != null) {
            filtros.add(cb.equal(book.get("yearPublication"), year));
        }
        query.select(book).where(filtros.toArray(Predicate[]::new));
        query.orderBy(ordenes(cb, query, book, pageable.getSort()));

        TypedQuery<Book> contenido = em.createQuery(query);
        if (pageable.isPaged()) {
            contenido.setFirstResult((int) pageable.getOffset());
            contenido.setMaxResults(pageable.getPageSize());
        }
        List<Book> filas = contenido.getResultList();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Book> base = conteo.from(Book.class);
        List<Predicate> filtrosConteo = new ArrayList<>();
        filtrosConteo.add(base.get("status").get("id").in(statusIds));
        if (q != null && !q.isBlank()) {
            filtrosConteo.add(textoPredicate(cb, base, q));
        }
        if (year != null) {
            filtrosConteo.add(cb.equal(base.get("yearPublication"), year));
        }
        conteo.select(cb.count(base)).where(filtrosConteo.toArray(Predicate[]::new));
        long total = em.createQuery(conteo).getSingleResult();
        return new PageImpl<>(filas, pageable, total);
    }

    @Override
    public List<Book> suggestByTitleCriteria(String text, Integer statusId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);
        var similitud = cb.function("similarity", Double.class,
                book.get("title"), cb.literal(text));
        query.select(book).where(
                cb.equal(book.get("status").get("id"), statusId),
                cb.greaterThan(similitud, 0.1));
        query.orderBy(cb.desc(similitud));
        return em.createQuery(query).setMaxResults(10).getResultList();
    }

    private Predicate textoPredicate(CriteriaBuilder cb, Root<Book> book, String q) {
        String patron = "%" + q.toLowerCase() + "%";
        return cb.or(
                cb.like(cb.lower(book.get("title")), patron),
                cb.like(cb.lower(book.get("isbn")), patron));
    }

    private List<Order> ordenes(CriteriaBuilder cb, CriteriaQuery<Book> query,
            Root<Book> book, Sort sort) {
        List<Order> resultado = new ArrayList<>();
        for (Sort.Order orden : sort) {
            var propiedad = book.get(orden.getProperty());
            resultado.add(orden.isAscending() ? cb.asc(propiedad) : cb.desc(propiedad));
        }
        if (resultado.isEmpty()) {
            resultado.add(cb.asc(book.get("id")));
        }
        return resultado;
    }
}
