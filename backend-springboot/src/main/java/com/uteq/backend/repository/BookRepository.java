package com.uteq.backend.repository;

import com.uteq.backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

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

    // Nota P5: las 6 queries nativas de búsqueda (texto/ISBN/categoría/
    // autor/sugerencia/pendientes/estados) migraron a BookRepositoryCustom
    // (Criteria API). isbn es character varying(13) en el schema vigente y
    // la entidad lo mapea como String, por lo que LOWER directo es seguro.

}
