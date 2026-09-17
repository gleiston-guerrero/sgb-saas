package com.uteq.backend.repository;

import com.uteq.backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Busca un libro por su ISBN exacto.
     *
     * @param isbn ISBN a buscar
     * @return el libro si existe
     */
    Optional<Book> findByIsbn(String isbn);

    // "activo" ya no existe como columna: el estado ACTIVO/DADO_DE_BAJA/...
    // vive en estados_libro (ver Libro.estado). findByEstado_Nombre navega
    // esa relación por nombre en vez de hardcodear el id del catálogo.
    /**
     * Lista libros del estado dado, paginado.
     *
     * @param statusName nombre del estado a filtrar
     * @param pageable paginación y orden
     * @return página de libros en ese estado
     */
    Page<Book> findByStatus_Name(String statusName, Pageable pageable);

    /**
     * Busca libros cuyo título contenga el texto y estén en el estado dado.
     *
     * @param title texto a buscar dentro del título
     * @param statusName nombre del estado a filtrar
     * @return lista de libros coincidentes
     */
    List<Book> findByTitleContainingIgnoreCaseAndStatus_Name(String title, String statusName);

    /**
     * Indica si ya existe un libro con el ISBN dado.
     *
     * @param isbn ISBN a verificar
     * @return verdadero si existe
     */
    boolean existsByIsbn(String isbn);

    /**
     * Indica si otro libro distinto ya usa el ISBN dado.
     *
     * @param isbn ISBN a verificar
     * @param id identificador del libro a excluir
     * @return verdadero si otro libro lo usa
     */
    boolean existsByIsbnAndIdNot(String isbn, Long id);

    // Filtros de catálogo por categoría/autor (navega las colecciones @ManyToMany de Libro).
    /**
     * Lista libros de la categoría y estado dados (por nombre de estado).
     *
     * @param categoryId identificador de la categoría
     * @param statusName nombre del estado a filtrar
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByCategories_IdAndStatus_Name(Integer categoryId, String statusName, Pageable pageable);

    /**
     * Lista libros del autor y estado dados (por nombre de estado).
     *
     * @param authorId identificador del autor
     * @param statusName nombre del estado a filtrar
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByAuthors_IdAndStatus_Name(Long authorId, String statusName, Pageable pageable);

    // Filtros de libros (título/ISBN + categoría + autor + estado)
    /**
     * Lista libros del estado dado, paginado.
     *
     * @param statusId identificador del estado a filtrar
     * @param pageable paginación y orden
     * @return página de libros en ese estado
     */
    Page<Book> findByStatusId(Integer statusId, Pageable pageable);

    /**
     * Lista libros del estado dado con existencias disponibles.
     *
     * @param statusId identificador del estado a filtrar
     * @param stock umbral de existencias disponibles
     * @param pageable paginación y orden
     * @return página de libros con stock mayor al umbral
     */
    Page<Book> findByStatusIdAndStockAvailableGreaterThan(Integer statusId, int stock, Pageable pageable);

    /**
     * Lista libros del estado dado sin existencias disponibles.
     *
     * @param statusId identificador del estado a filtrar
     * @param stock umbral de existencias disponibles
     * @param pageable paginación y orden
     * @return página de libros con stock igual al umbral
     */
    Page<Book> findByStatusIdAndStockAvailableEquals(Integer statusId, int stock, Pageable pageable);

    /**
     * Lista libros de la categoría y estado dados.
     *
     * @param categoryId identificador de la categoría
     * @param statusId identificador del estado
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByCategories_IdAndStatusId(Integer categoryId, Integer statusId, Pageable pageable);

    /**
     * Lista libros de la categoría y estado dados con existencias disponibles.
     *
     * @param categoryId identificador de la categoría
     * @param statusId identificador del estado
     * @param stock umbral de existencias disponibles
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByCategories_IdAndStatusIdAndStockAvailableGreaterThan(Integer categoryId, Integer statusId, int stock, Pageable pageable);

    /**
     * Lista libros de la categoría y estado dados sin existencias disponibles.
     *
     * @param categoryId identificador de la categoría
     * @param statusId identificador del estado
     * @param stock umbral de existencias disponibles
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByCategories_IdAndStatusIdAndStockAvailableEquals(Integer categoryId, Integer statusId, int stock, Pageable pageable);

    /**
     * Lista libros del autor y estado dados.
     *
     * @param authorId identificador del autor
     * @param statusId identificador del estado
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByAuthors_IdAndStatusId(Long authorId, Integer statusId, Pageable pageable);

    /**
     * Lista libros de la categoría, autor y estado dados.
     *
     * @param categoryId identificador de la categoría
     * @param authorId identificador del autor
     * @param statusId identificador del estado
     * @param pageable paginación y orden
     * @return página de libros coincidentes
     */
    Page<Book> findByCategories_IdAndAuthors_IdAndStatusId(Integer categoryId, Long authorId, Integer statusId, Pageable pageable);

    // --- Queries nativas: isbn puede ser bytea o varchar en BD real ---
    // Todas usan isbn::text para compatibilidad con ambos tipos (bytea y varchar).
    // Las queries anteriores eran JPQL con LOWER(l.isbn) que fallaba si
    // isbn es bytea ("function lower(bytea) does not exist").
    //
    // Revisadas de nuevo para P4 (nativeQuery -> JPQL): se mantienen
    // nativas a proposito. schema.sql declara isbn character varying(13)
    // hoy, pero el fallo LOWER(bytea) documentado arriba fue real contra
    // una instancia con drift de tipo -- no hay migracion que garantice
    // que toda base desplegada (incluida Neon en produccion) esta en el
    // tipo actual. El cast ::text y similarity() (pg_trgm, en
    // suggestByTitle) son ademas sintaxis especifica de PostgreSQL sin
    // equivalente portable en JPQL. Revertir a JPQL aqui arriesgaria
    // reproducir el incidente ya documentado a cambio de una anotacion;
    // no es un cambio de bajo riesgo.

    // Búsqueda por título O ISBN con estado específico + filtro opcional disponible
    @Query(value = "SELECT l.* FROM libros l "
            + "WHERE l.estado_id = :statusId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))"
            + "AND (:available IS NULL OR (:available = true AND l.stock_disponible > 0) OR (:available = false AND l.stock_disponible = 0))",
            countQuery = "SELECT count(*) FROM libros l "
            + "WHERE l.estado_id = :statusId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))"
            + "AND (:available IS NULL OR (:available = true AND l.stock_disponible > 0) OR (:available = false AND l.stock_disponible = 0))",
            nativeQuery = true)
    Page<Book> searchByTextOIsbn(@Param("q") String q, @Param("statusId") Integer statusId, @Param("available") Boolean available, Pageable pageable);

    // Búsqueda por título O ISBN + categoría (+ disponible)
    @Query(value = "SELECT l.* FROM libros l "
            + "INNER JOIN libro_categorias lc ON lc.libro_id = l.id "
            + "WHERE l.estado_id = :statusId "
            + "AND lc.categoria_id = :categoryId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))"
            + "AND (:available IS NULL OR (:available = true AND l.stock_disponible > 0) OR (:available = false AND l.stock_disponible = 0))",
            countQuery = "SELECT count(*) FROM libros l "
            + "INNER JOIN libro_categorias lc ON lc.libro_id = l.id "
            + "WHERE l.estado_id = :statusId "
            + "AND lc.categoria_id = :categoryId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))"
            + "AND (:available IS NULL OR (:available = true AND l.stock_disponible > 0) OR (:available = false AND l.stock_disponible = 0))",
            nativeQuery = true)
    Page<Book> searchByTextOIsbnYCategory(@Param("q") String q, @Param("categoryId") Integer categoryId, @Param("statusId") Integer statusId, @Param("available") Boolean available, Pageable pageable);

    // Búsqueda por título O ISBN + autor
    @Query(value = "SELECT l.* FROM libros l "
            + "INNER JOIN libro_autores la ON la.libro_id = l.id "
            + "WHERE l.estado_id = :statusId "
            + "AND la.autor_id = :authorId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))",
            countQuery = "SELECT count(*) FROM libros l "
            + "INNER JOIN libro_autores la ON la.libro_id = l.id "
            + "WHERE l.estado_id = :statusId "
            + "AND la.autor_id = :authorId "
            + "AND (LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', :q, '%')))",
            nativeQuery = true)
    Page<Book> searchByTextOrIsbnAndAuthor(@Param("q") String q, @Param("authorId") Long authorId, @Param("statusId") Integer statusId, Pageable pageable);

    // Búsqueda por similitud con pg_trgm (top 10 por similarity de título, para autocompletado).
    @Query(value = "SELECT * FROM libros "
            + "WHERE estado_id = :p_estado_id AND similarity(titulo, :p_texto) > 0.1 "
            + "ORDER BY similarity(titulo, :p_texto) DESC "
            + "LIMIT 10", nativeQuery = true)
    List<Book> suggestByTitle(@Param("p_texto") String text, @Param("p_estado_id") Integer statusId);

    // buscarPendientes y buscarPorEstados: nativas con isbn::text.
    // Antes usaban @EntityGraph pero eso no funciona con nativeQuery.
    // Hibernate crea proxies para las relaciones lazy (editorial, idioma,
    // estado, categorias, autores) que se resuelven bajo la transacción
    // @Transactional del service que llama.
    @Query(value = "SELECT l.* FROM libros l "
            + "WHERE l.estado_id = :statusId "
            + "AND ( :q IS NULL "
            + "      OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) "
            + "      OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) ) "
            + "AND ( :year IS NULL OR l.anio_publicacion = :year )",
            countQuery = "SELECT count(*) FROM libros l "
                    + "WHERE l.estado_id = :statusId "
                    + "AND ( :q IS NULL "
                    + "      OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) "
                    + "      OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) ) "
                    + "AND ( :year IS NULL OR l.anio_publicacion = :year )",
            nativeQuery = true)
    Page<Book> searchPendientes(@Param("q") String q, @Param("year") Short year, @Param("statusId") Integer statusId, Pageable pageable);

    @Query(value = "SELECT l.* FROM libros l "
            + "WHERE l.estado_id IN :statusIds "
            + "AND ( :q IS NULL "
            + "      OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) "
            + "      OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) ) "
            + "AND ( :year IS NULL OR l.anio_publicacion = :year )",
            countQuery = "SELECT count(*) FROM libros l "
                    + "WHERE l.estado_id IN :statusIds "
                    + "AND ( :q IS NULL "
                    + "      OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) "
                    + "      OR LOWER(l.isbn::text) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) ) "
                    + "AND ( :year IS NULL OR l.anio_publicacion = :year )",
            nativeQuery = true)
    Page<Book> searchByStatuses(@Param("statusIds") List<Integer> statusIds, @Param("q") String q, @Param("year") Short year, Pageable pageable);
}
