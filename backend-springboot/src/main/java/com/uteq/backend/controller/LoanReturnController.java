package com.uteq.backend.controller;

import com.uteq.backend.dto.LoanReturnFullResponseDTO;
import com.uteq.backend.dto.LoanReturnHistoryDTO;
import com.uteq.backend.dto.LoanReturnRequestDTO;
import com.uteq.backend.entity.User;
import com.uteq.backend.repository.UserRepository;
import com.uteq.backend.service.LoanReturnService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devoluciones")
public class LoanReturnController {

    private final LoanReturnService loanReturnService;
    private final UserRepository userRepo;

    /**
     * Constructor con el servicio de devoluciones y el repositorio de usuarios.
     *
     * @param loanReturnService servicio de devoluciones con inspección de daños
     * @param userRepo repositorio para resolver el id del bibliotecario por correo
     */
    public LoanReturnController(LoanReturnService loanReturnService,
                                UserRepository userRepo) {
        this.loanReturnService = loanReturnService;
        this.userRepo = userRepo;
    }
    /**
     * Registra la devolución de un préstamo con inspección de estado y posibles daños y multas.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN. Registra al bibliotecario autenticado.
     *
     * @param loanId id del préstamo que se devuelve
     * @param dto estado del ejemplar y observaciones de la devolución
     * @param authentication identidad del bibliotecario que registra la devolución
     * @return devolución completa con daños y multa si corresponden
     */
    @PostMapping("/prestamo/{prestamoId}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<LoanReturnFullResponseDTO> registerLoanReturn(
            @PathVariable("prestamoId") Long loanId,
            @Valid @RequestBody LoanReturnRequestDTO dto,
            Authentication authentication) {
        Long librarianId = resolveIdByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanReturnService.registerLoanReturn(loanId, dto, librarianId));
    }
    /**
     * Lista el historial de devoluciones registradas por el bibliotecario autenticado.
     * Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @param authentication identidad del bibliotecario cuyo historial se consulta
     * @return lista de devoluciones registradas por ese bibliotecario
     */
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<LoanReturnHistoryDTO>> historyLoanReturns(
            Authentication authentication) {
        Long librarianId = resolveIdByEmail(authentication.getName());
        return ResponseEntity.ok(loanReturnService.historyLoanReturns(librarianId));
    }

    private Long resolveIdByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        return user.getId();
    }
}
