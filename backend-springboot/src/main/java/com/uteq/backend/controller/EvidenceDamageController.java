package com.uteq.backend.controller;

import com.uteq.backend.dto.EvidenceDamageResponseDTO;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.UserRepository;
import com.uteq.backend.service.LoanReturnService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devoluciones")
public class EvidenceDamageController {

    private final LoanReturnService loanReturnService;
    private final UserRepository userRepo;

    /**
     * Constructor con el servicio de devoluciones y el repositorio de usuarios.
     *
     * @param loanReturnService servicio de devoluciones y evidencias de daño
     * @param userRepo repositorio para resolver el id del bibliotecario por correo
     */
    public EvidenceDamageController(LoanReturnService loanReturnService,
                                    UserRepository userRepo) {
        this.loanReturnService = loanReturnService;
        this.userRepo = userRepo;
    }
    /**
     * Sube una foto de evidencia para un registro de daño de devolución.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Registra al bibliotecario autenticado como autor.
     *
     * @param registrationDamageId id del registro de daño al que se anexa la foto
     * @param file archivo de imagen de la evidencia
     * @param authentication identidad del bibliotecario que sube la evidencia
     * @return evidencia guardada con sus metadatos
     */

    @PostMapping(value = "/evidencia/{registroDanoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<EvidenceDamageResponseDTO> uploadEvidence(
            @PathVariable("registroDanoId") Long registrationDamageId,
            @RequestParam("archivo") MultipartFile file,
            Authentication authentication) {
        Long librarianId = resolveIdByEmail(authentication.getName());
        return ResponseEntity.ok(loanReturnService.uploadEvidence(registrationDamageId, file, librarianId));
    }
    /**
     * Lista las evidencias fotográficas anexadas a un registro de daño.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param registrationDamageId id del registro de daño cuyas evidencias se consultan
     * @return lista de evidencias del registro indicado
     */
    @GetMapping("/evidencia/{registroDanoId}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<EvidenceDamageResponseDTO>> listEvidences(
            @PathVariable("registroDanoId") Long registrationDamageId) {
        return ResponseEntity.ok(loanReturnService.listEvidences(registrationDamageId));
    }
    /**
     * Descarga el binario de una evidencia de daño con su tipo de contenido original.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param id id de la evidencia cuyo archivo se solicita
     * @return bytes del archivo con su tipo de contenido
     */
    @GetMapping("/evidencia/{id}/archivo")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        var evidence = loanReturnService.getFileBinary(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidence.fileType()))
                .body(evidence.fileBytes());
    }

    private Long resolveIdByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        return user.getId();
    }
}
