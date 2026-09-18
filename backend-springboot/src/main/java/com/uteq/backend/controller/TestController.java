package com.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {
    /**
     * Devuelve un mensaje de prueba para verificar que la autenticación funciona.
     *
     * @return mensaje de confirmación de acceso autenticado
     */
    @GetMapping("/protegido")
    public ResponseEntity<String> protectedEndpoint() {
        return ResponseEntity.ok("Acceso autorizado. Estás autenticado correctamente.");
    }
}
