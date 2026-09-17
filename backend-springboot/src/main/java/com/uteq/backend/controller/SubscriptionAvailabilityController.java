package com.uteq.backend.controller;

import com.uteq.backend.repository.UserRepository;
import com.uteq.backend.service.SubscriptionAvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/libros")
public class SubscriptionAvailabilityController {

    private final SubscriptionAvailabilityService service;
    private final UserRepository userRepo;

    public SubscriptionAvailabilityController(SubscriptionAvailabilityService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }
    /**
     * Procesa suscribir y devuelve el resultado calculado por el backend.
     *
     * @param bookId identificador del registro que se usa para ubicar el recurso en la base de datos
     * @param auth identidad autenticada usada para aplicar permisos y registrar autoria de la accion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @PostMapping("/{libroId}/suscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subscribe(@PathVariable("libroId") Long bookId, Authentication auth) {
        Long userId = resolveUserId(auth);
        service.subscribe(userId, bookId);
        return ResponseEntity.ok().build();
    }
    /**
     * Procesa desuscribir y devuelve el resultado calculado por el backend.
     *
     * @param bookId identificador del registro que se usa para ubicar el recurso en la base de datos
     * @param auth identidad autenticada usada para aplicar permisos y registrar autoria de la accion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @DeleteMapping("/{libroId}/suscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unsubscribe(@PathVariable("libroId") Long bookId, Authentication auth) {
        Long userId = resolveUserId(auth);
        service.unsubscribe(userId, bookId);
        return ResponseEntity.noContent().build();
    }
    /**
     * Procesa mis subscriptions y devuelve el resultado calculado por el backend.
     *
     * @param auth identidad autenticada usada para aplicar permisos y registrar autoria de la accion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @GetMapping("/suscripciones/mias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Long>> mySubscriptions(Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(service.listBooksIds(userId));
    }

    private Long resolveUserId(Authentication auth) {
        String email = auth.getName();
        try {
            return Long.parseLong(email);
        } catch (NumberFormatException e) {
            return userRepo.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email))
                    .getId();
        }
    }
}
