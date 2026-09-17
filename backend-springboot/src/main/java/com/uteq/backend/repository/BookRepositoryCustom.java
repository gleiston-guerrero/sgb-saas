package com.uteq.backend.repository;

import com.uteq.backend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Búsquedas del catálogo con filtros opcionales vía Criteria API (P5).
 *
 * <p>Reemplaza las queries nativas de {@code BookRepository} afectadas por
 * dos limitaciones: el bug PostgreSQL + parámetro NULL en JPQL (filtros
 * opcionales con {@code :p IS NULL} fallan con <i>could not determine data
 * type</i>) y los JOINs a {@code libro_categorias}/{@code libro_autores}.
 * Los predicados se agregan solo cuando el filtro viene informado, por lo
 * que un filtro nulo equivale a "sin filtro" (mismo contrato observable
 * que las nativas con guardas NULL).
 */
public interface BookRepositoryCustom {

    /**
     * Búsqueda por título o ISBN con estado, categoría/autor y
     * disponibilidad opcionales.
     *
     * @param q texto no vacío para título o ISBN
     * @param statusId estado exigido
     * @param categoryId categoría exigida, nulo = todas
     * @param authorId autor exigido, nulo = todos
     * @param available true = con stock, false = agotados, nulo = ambos
     * @param pageable paginación con propiedades de entidad (ver
     *        BookService: SORT_TO_PROPERTY)
     * @return página de libros (contenido distinto, total exacto)
     */
    Page<Book> searchText(String q, Integer statusId, Integer categoryId,
            Long authorId, Boolean available, Pageable pageable);

    /**
     * Bandeja de gestión: estados dados con texto y año opcionales.
     *
     * @param statusIds estados a incluir (no vacío)
     * @param q texto para título o ISBN, nulo o vacío = sin filtro
     * @param year año de publicación, nulo = todos
     * @param pageable paginación con propiedades de entidad
     * @return página de libros
     */
    Page<Book> searchByStatusesCriteria(List<Integer> statusIds, String q,
            Short year, Pageable pageable);

    /**
     * Autocompletado por similitud de título (pg_trgm vía
     * {@code function('similarity', ...)}).
     *
     * @param text texto escrito por quien busca
     * @param statusId estado exigido
     * @return hasta 10 libros ordenados por similitud descendente
     */
    List<Book> suggestByTitleCriteria(String text, Integer statusId);
}
