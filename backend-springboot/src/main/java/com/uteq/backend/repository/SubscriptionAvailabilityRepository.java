package com.uteq.backend.repository;

import com.uteq.backend.entity.SubscriptionAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubscriptionAvailabilityRepository extends JpaRepository<SubscriptionAvailability, Long> {
    /** Indica si el usuario dado está suscrito a la disponibilidad del libro dado. */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    /** Busca la suscripción del usuario y libro dados, si existe. */
    Optional<SubscriptionAvailability> findByUserIdAndBookId(Long userId, Long bookId);
    /** Lista las suscripciones al libro dado. */
    List<SubscriptionAvailability> findByBookId(Long bookId);
    /** Lista las suscripciones del usuario dado. */
    List<SubscriptionAvailability> findByUserId(Long userId);
    /** Elimina la suscripción del usuario y libro dados. */
    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
