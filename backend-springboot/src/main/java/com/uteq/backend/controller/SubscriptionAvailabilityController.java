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

    /**
     * Constructor con el servicio de suscripciones y el repositorio de usuarios.
     *
     * @param service servicio de suscripciones de disponibilidad por libro
     * @param userRepo repositorio para resolver el id del usuario por correo
     */
    public SubscriptionAvailabilityController(SubscriptionAvailabilityService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }
    /**
     * Suscribe al usuario autenticado a los avisos de disponibilidad de un libro.
     * Cualquier usuario autenticado puede suscribirse.
     *
     * @param bookId id del libro al que se suscribe
     * @param auth identidad autenticada que pide el aviso
     * @return respuesta vacía con estado 200 si se registró
     */
    @PostMapping("/{libroId}/suscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subscribe(@PathVariable("libroId") Long bookId, Authentication auth) {
        Long userId = resolveUserId(auth);
        service.subscribe(userId, bookId);
        return ResponseEntity.ok().build();
    }
    /**
     * Cancela la suscripción del usuario autenticado a los avisos de un libro.
     * Cualquier usuario autenticado puede desuscribirse.
     *
     * @param bookId id del libro del que se desuscribe
     * @param auth identidad autenticada que cancela el aviso
     * @return respuesta vacía con estado 204 si se canceló
     */
    @DeleteMapping("/{libroId}/suscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unsubscribe(@PathVariable("libroId") Long bookId, Authentication auth) {
        Long userId = resolveUserId(auth);
        service.unsubscribe(userId, bookId);
        return ResponseEntity.noContent().build();
    }
    /**
     * Devuelve los ids de libros a los que está suscrito el usuario autenticado.
     * Cualquier usuario autenticado puede consultar los suyos.
     *
     * @param auth identidad autenticada cuyas suscripciones se consultan
     * @return lista de ids de libros con suscripción vigente
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
