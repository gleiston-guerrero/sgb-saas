package com.uteq.backend.service;

import com.uteq.backend.entity.Book;
import com.uteq.backend.entity.SubscriptionAvailability;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.SubscriptionAvailabilityRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionAvailabilityServiceTest {

    @Mock SubscriptionAvailabilityRepository subscriptionRepo;
    @Mock UserRepository userRepo;
    @Mock BookRepository bookRepo;
    @Mock NotificationService notificationService;

    @InjectMocks SubscriptionAvailabilityService service;

    // ── Test 1: suscripción nueva con stock disponible notifica de inmediato ──
    @Test
    void subscribe_bookAvailable_guardaYNotifica() {
        given(userRepo.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(bookRepo.findById(3L)).willReturn(Optional.of(bookWithStock(3L, "Clean Code", 2)));
        given(subscriptionRepo.existsByUserIdAndBookId(1L, 3L)).willReturn(false);

        service.subscribe(1L, 3L);

        ArgumentCaptor<SubscriptionAvailability> captor =
                ArgumentCaptor.forClass(SubscriptionAvailability.class);
        verify(subscriptionRepo).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getBookId()).isEqualTo(3L);
        assertThat(captor.getValue().getCreated()).isNotNull();
        verify(notificationService).notifyBookAvailable(1L, 3L, "Clean Code");
    }

    // ── Test 2: ya suscrito -> salida temprana sin guardar ni notificar ──
    @Test
    void subscribe_alreadySubscribed_notGuardaNiNotifica() {
        given(userRepo.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(bookRepo.findById(3L)).willReturn(Optional.of(bookWithStock(3L, "Clean Code", 2)));
        given(subscriptionRepo.existsByUserIdAndBookId(1L, 3L)).willReturn(true);

        service.subscribe(1L, 3L);

        verify(subscriptionRepo, never()).save(any());
        verify(notificationService, never()).notifyBookAvailable(any(), any(), any());
    }

    // ── Test 3: sin stock -> guarda pero no notifica ──
    @Test
    void subscribe_withoutStock_guardaWithoutNotificar() {
        given(userRepo.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(bookRepo.findById(3L)).willReturn(Optional.of(bookWithStock(3L, "Clean Code", 0)));
        given(subscriptionRepo.existsByUserIdAndBookId(1L, 3L)).willReturn(false);

        service.subscribe(1L, 3L);

        verify(subscriptionRepo).save(any());
        verify(notificationService, never()).notifyBookAvailable(any(), any(), any());
    }

    // ── Test 4: usuario inexistente -> 404 ──
    @Test
    void subscribe_userInexistente_lanzaEntityNotFound() {
        given(userRepo.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(99L, 3L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── Test 5: libro inexistente -> 404 ──
    @Test
    void subscribe_bookInexistente_lanzaEntityNotFound() {
        given(userRepo.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(bookRepo.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(1L, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── Test 6: desuscribir delega el borrado ──
    @Test
    void unsubscribe_existing_delegaBorrado() {
        service.unsubscribe(1L, 3L);

        verify(subscriptionRepo).deleteByUserIdAndBookId(1L, 3L);
    }

    // ── Test 7: listado de libros suscritos del usuario ──
    @Test
    void listBooksIds_withSubscriptions_retornaSoloIds() {
        SubscriptionAvailability s1 = new SubscriptionAvailability();
        s1.setUserId(1L);
        s1.setBookId(3L);
        SubscriptionAvailability s2 = new SubscriptionAvailability();
        s2.setUserId(1L);
        s2.setBookId(7L);
        given(subscriptionRepo.findByUserId(1L)).willReturn(List.of(s1, s2));

        assertThat(service.listBooksIds(1L)).containsExactly(3L, 7L);
    }

    // ── Test 8: libro con stock notifica a cada suscriptor y limpia ──
    @Test
    void notifyAvailable_withStock_notificaYEliminaSubscriptions() {
        SubscriptionAvailability s1 = new SubscriptionAvailability();
        s1.setUserId(1L);
        s1.setBookId(3L);
        SubscriptionAvailability s2 = new SubscriptionAvailability();
        s2.setUserId(2L);
        s2.setBookId(3L);
        given(bookRepo.findById(3L)).willReturn(Optional.of(bookWithStock(3L, "Clean Code", 4)));
        given(subscriptionRepo.findByBookId(3L)).willReturn(List.of(s1, s2));

        service.notifyAvailable(3L);

        verify(notificationService).notifyBookAvailable(1L, 3L, "Clean Code");
        verify(notificationService).notifyBookAvailable(2L, 3L, "Clean Code");
        verify(subscriptionRepo).delete(s1);
        verify(subscriptionRepo).delete(s2);
    }

    // ── Test 9: libro sin stock -> salida temprana sin tocar suscripciones ──
    @Test
    void notifyAvailable_withoutStock_notTocaSubscriptions() {
        given(bookRepo.findById(3L)).willReturn(Optional.of(bookWithStock(3L, "Clean Code", 0)));

        service.notifyAvailable(3L);

        verify(subscriptionRepo, never()).findByBookId(any());
        verify(notificationService, never()).notifyBookAvailable(any(), any(), any());
    }

    // ── Test 10: libro inexistente -> 404 ──
    @Test
    void notifyAvailable_bookInexistente_lanzaEntityNotFound() {
        given(bookRepo.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.notifyAvailable(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── Test 11: stock nulo no notifica (solo guarda) ──
    @Test
    void subscribe_stockNulo_guardaWithoutNotificar() {
        Book book = bookWithStock(3L, "Clean Code", 0);
        book.setStockAvailable(null);
        given(userRepo.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(bookRepo.findById(3L)).willReturn(Optional.of(book));
        given(subscriptionRepo.existsByUserIdAndBookId(1L, 3L)).willReturn(false);

        service.subscribe(1L, 3L);

        verify(subscriptionRepo).save(any());
        verify(notificationService, never()).notifyBookAvailable(any(), any(), any());
    }

    // ── Test 12: stock nulo en aviso masivo sale temprano ──
    @Test
    void notifyAvailable_stockNulo_notTocaSubscriptions() {
        Book book = bookWithStock(3L, "Clean Code", 0);
        book.setStockAvailable(null);
        given(bookRepo.findById(3L)).willReturn(Optional.of(book));

        service.notifyAvailable(3L);

        verify(subscriptionRepo, never()).findByBookId(any());
        verify(notificationService, never()).notifyBookAvailable(any(), any(), any());
    }

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Book bookWithStock(Long id, String title, int stock) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setStockAvailable((short) stock);
        return book;
    }
}
