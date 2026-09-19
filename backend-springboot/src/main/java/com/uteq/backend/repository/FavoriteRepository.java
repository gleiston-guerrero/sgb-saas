package com.uteq.backend.repository;

import com.uteq.backend.entity.Favorite;
import com.uteq.backend.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// JpaRepository<Favorito, FavoritoId>: la PK compuesta (@IdClass en
// Favorito) se referencia acá con la clase auxiliar FavoritoId, no con
// Long -- mismo mecanismo que cualquier entidad con @IdClass en Spring
// Data JPA.
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    /**
     * Lista los favoritos del usuario dado.
     *
     * @param userId identificador del usuario.
     * @return favoritos asociados al usuario.
     */
    List<Favorite> findByUserId(Long userId);

    /**
     * Pagina los favoritos del usuario dado.
     *
     * @param userId identificador del usuario.
     * @param pageable configuración de paginación y orden.
     * @return página de favoritos asociados al usuario.
     */
    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    /**
     * Indica si el libro dado está en los favoritos del usuario dado.
     *
     * @param userId identificador del usuario.
     * @param bookId identificador del libro.
     * @return {@code true} si el libro pertenece a los favoritos del usuario.
     */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Elimina el favorito del usuario y libro dados.
     *
     * @param userId identificador del usuario.
     * @param bookId identificador del libro.
     */
    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
