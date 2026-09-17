package com.uteq.backend.service;

import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.SubscriptionAvailability;
import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.SubscriptionAvailabilityRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SubscriptionAvailabilityService {

    private final SubscriptionAvailabilityRepository subscriptionRepo;
    private final UserRepository userRepo;
    private final BookRepository bookRepo;
    private final NotificationService notificationService;

    /**
     * Constructor con los repositorios de suscripciones, usuarios y libros más las notificaciones.
     *
     * @param subscriptionRepo repositorio de suscripciones de disponibilidad por usuario y libro
     * @param userRepo repositorio de usuarios para validar al suscriptor
     * @param bookRepo repositorio de libros para validar el título y su stock actual
     * @param notificationService servicio que avisa al suscriptor cuando el libro está disponible
     */
    public SubscriptionAvailabilityService(SubscriptionAvailabilityRepository subscriptionRepo,
                                            UserRepository userRepo,
                                            BookRepository bookRepo,
                                            NotificationService notificationService) {
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo = userRepo;
        this.bookRepo = bookRepo;
        this.notificationService = notificationService;
    }
    /**
     * Suscribe a un lector para avisarle cuando un libro agotado vuelva a tener stock.
     * Es idempotente: si ya existe la suscripción no hace nada. Cuando el libro ya tiene
     * disponibles, avisa de inmediato en lugar de esperar la reposición.
     *
     * @param userId identificador del lector suscriptor, debe existir
     * @param bookId identificador del libro agotado o disponible a vigilar, debe existir
     * @throws jakarta.persistence.EntityNotFoundException si el usuario o el libro no existen
     */
    @Transactional
    public void subscribe(Long userId, Long bookId) {
        userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + userId));
        Book book = bookRepo.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Libro no encontrado: " + bookId));
        if (subscriptionRepo.existsByUserIdAndBookId(userId, bookId)) {
            return;
        }
        SubscriptionAvailability s = new SubscriptionAvailability();
        s.setUserId(userId);
        s.setBookId(bookId);
        s.setCreated(OffsetDateTime.now());
        subscriptionRepo.save(s);
        // Si ya esta disponible, notificar inmediato
        if (book.getStockAvailable() != null && book.getStockAvailable() > 0) {
            notificationService.notifyBookAvailable(userId, bookId, book.getTitle());
        }
    }
    /**
     * Cancela la suscripción de un lector a la disponibilidad de un libro para dejar de avisarle.
     * No falla si la suscripción ya no existe.
     *
     * @param userId identificador del lector que deja de vigilar el libro
     * @param bookId identificador del libro que deja de vigilarse
     */
    @Transactional
    public void unsubscribe(Long userId, Long bookId) {
        subscriptionRepo.deleteByUserIdAndBookId(userId, bookId);
    }
    /**
     * Lista los libros que un lector vigila para mostrar su campana de disponibilidad.
     *
     * @param userId identificador del lector suscriptor cuyas vigilancias se consultan
     * @return identificadores de los libros vigilados por ese lector
     */
    @Transactional(readOnly = true)
    public List<Long> listBooksIds(Long userId) {
        return subscriptionRepo.findByUserId(userId).stream().map(SubscriptionAvailability::getBookId).toList();
    }
    /**
     * Avisa a todos los suscriptores de un libro que volvió a tener stock y consume sus suscripciones.
     * Se dispara al actualizar un libro de cero a disponible; no hace nada si el libro sigue agotado.
     *
     * @param bookId identificador del libro repuesto cuyo stock volvió a ser mayor a cero
     * @throws jakarta.persistence.EntityNotFoundException si el libro no existe
     */
    @Transactional
    public void notifyAvailable(Long bookId) {
        Book book = bookRepo.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Libro no encontrado: " + bookId));
        if (book.getStockAvailable() == null || book.getStockAvailable() <= 0) return;
        List<SubscriptionAvailability> subs = subscriptionRepo.findByBookId(bookId);
        for (SubscriptionAvailability s : subs) {
            notificationService.notifyBookAvailable(s.getUserId(), bookId, book.getTitle());
            subscriptionRepo.delete(s);
        }
    }
}
