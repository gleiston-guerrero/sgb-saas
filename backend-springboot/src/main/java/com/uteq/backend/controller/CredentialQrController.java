package com.uteq.backend.controller;

import com.uteq.backend.service.CredentialQrService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Modulo 8: sin parametro de usuarioId en la URL a proposito -- cada
// LECTOR ve UNICAMENTE su propia credencial, resuelta a partir del
// Authentication, para que no sea posible pedir el QR de otro usuario
// cambiando un id en la URL.
@RestController
@RequestMapping("/api/v1/credencial-qr")
public class CredentialQrController {

    private final CredentialQrService service;

    /**
     * Constructor con el servicio de credenciales QR.
     *
     * @param service servicio de generación de la imagen QR de credencial
     */
    public CredentialQrController(CredentialQrService service) {
        this.service = service;
    }
    /**
     * Genera la imagen QR de la credencial del propio LECTOR autenticado.
     * Solo LECTOR. El usuario se resuelve del Authentication, sin id en la URL.
     *
     * @param authentication identidad del LECTOR que pide su credencial
     * @return bytes de la imagen PNG de la credencial para mostrar en línea
     */
    @GetMapping(value = "/mi-credencial", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<byte[]> myCredential(Authentication authentication) {
        byte[] image = service.generateImageQrOwn(authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"credencial-qr.png\"")
                .body(image);
    }
}
