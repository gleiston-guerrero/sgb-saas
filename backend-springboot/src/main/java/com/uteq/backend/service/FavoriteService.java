package com.uteq.backend.service;

import com.uteq.backend.dto.FavoriteResponseDTO;
import com.uteq.backend.entity.Favorite;
import com.uteq.backend.entity.Book;
import com.uteq.backend.repository.FavoriteRepository;
import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Favoritos del usuario autenticado: el dueño se resuelve siempre desde el Authentication.
@Service
public class FavoriteService {

    private static final String LIBRO_NO_ENCONTRADO = "Libro no encontrado con id: ";
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado con correo: ";
    private static final String FAVORITO_NO_ENCONTRADO = "El libro %d no está en favoritos del usuario autenticado.";

    private final FavoriteRepository favoriteRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;

    /**
     * Constructor con los repositorios de favoritos, libros y usuarios.
     *
     * @param favoriteRepo repositorio de favoritos por usuario y libro
     * @param bookRepo repositorio de libros para validar existencia y resolver títulos
     * @param userRepo repositorio de usuarios para resolver el dueño desde su correo
     */
    public FavoriteService(FavoriteRepository favoriteRepo,
                            BookRepository bookRepo,
                            UserRepository userRepo) {
        this.favoriteRepo = favoriteRepo;
        this.bookRepo = bookRepo;
        this.userRepo = userRepo;
    }
    /**
     * Marca un libro como favorito del usuario autenticado para su lista personal de lectura.
     * Es idempotente: si el libro ya está marcado devuelve el favorito existente sin duplicarlo.
     *
     * @param bookId identificador del libro a marcar como favorito
     * @param authentication identidad autenticada del dueño, resuelta siempre desde el correo del JWT
     * @return el favorito con usuario, libro, título y fecha en que se agregó
     * @throws jakarta.persistence.EntityNotFoundException si no existe el libro o el usuario autenticado
     */
    @Transactional
    public FavoriteResponseDTO add(Long bookId, Authentication authentication) {
        Long userId = resolveIdByEmail(authentication.getName());
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(LIBRO_NO_ENCONTRADO + bookId));

        if (favoriteRepo.existsByUserIdAndBookId(userId, bookId)) {
            // Idempotente: marcar dos veces devuelve el estado actual.
            Favorite existing = favoriteRepo.findByUserId(userId).stream()
                    .filter(f -> f.getBookId().equals(bookId))
                    .findFirst()
                    .orElseThrow();
            return toDTO(existing, book.getTitle());
        }

        Favorite favorite = favoriteRepo.save(new Favorite(userId, bookId));
        return toDTO(favorite, book.getTitle());
    }
    /**
     * Quita un libro de los favoritos del usuario autenticado para depurar su lista personal.
     *
     * @param bookId identificador del libro a quitar de favoritos
     * @param authentication identidad autenticada del dueño, resuelta siempre desde el correo del JWT
     * @throws jakarta.persistence.EntityNotFoundException si ese libro no está en favoritos del usuario
     */
    @Transactional
    public void remove(Long bookId, Authentication authentication) {
        Long userId = resolveIdByEmail(authentication.getName());
        if (!favoriteRepo.existsByUserIdAndBookId(userId, bookId)) {
            throw new EntityNotFoundException(String.format(FAVORITO_NO_ENCONTRADO, bookId));
        }
        favoriteRepo.deleteByUserIdAndBookId(userId, bookId);
    }
    /**
     * Lista todos los favoritos del usuario autenticado con el título actual de cada libro.
     * Si un libro se eliminó del catálogo su título llega nulo en lugar de romper el listado.
     *
     * @param authentication identidad autenticada del dueño, resuelta siempre desde el correo del JWT
     * @return favoritos propios con usuario, libro, título y fecha en que se agregó
     * @throws jakarta.persistence.EntityNotFoundException si el usuario autenticado ya no existe
     */
    @Transactional(readOnly = true)
    public List<FavoriteResponseDTO> listOwns(Authentication authentication) {
        Long userId = resolveIdByEmail(authentication.getName());
        return favoriteRepo.findByUserId(userId).stream()
                .map(f -> toDTO(f, title(f.getBookId())))
                .toList();
    }
    /**
     * Lista en forma paginada los favoritos del usuario autenticado para las vistas con scroll largo.
     * Resuelve el título de cada libro igual que el listado completo.
     *
     * @param authentication identidad autenticada del dueño, resuelta siempre desde el correo del JWT
     * @param pageable paginación, tamaño y orden solicitados por la vista de favoritos
     * @return página de favoritos propios con usuario, libro, título y fecha en que se agregó
     * @throws jakarta.persistence.EntityNotFoundException si el usuario autenticado ya no existe
     */
    @Transactional(readOnly = true)
    public Page<FavoriteResponseDTO> listOwnsPaginated(Authentication authentication, Pageable pageable) {
        Long userId = resolveIdByEmail(authentication.getName());
        return favoriteRepo.findByUserId(userId, pageable)
                .map(f -> toDTO(f, title(f.getBookId())));
    }

    private String title(Long bookId) {
        return bookRepo.findById(bookId).map(Book::getTitle).orElse(null);
    }

    private Long resolveIdByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + email))
                .getId();
    }

    private FavoriteResponseDTO toDTO(Favorite f, String titleBook) {
        return new FavoriteResponseDTO(f.getUserId(), f.getBookId(), titleBook, f.getAgregado());
    }
}
