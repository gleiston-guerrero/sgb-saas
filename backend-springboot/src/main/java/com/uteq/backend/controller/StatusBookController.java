package com.uteq.backend.controller;

import com.uteq.backend.dto.StatusBookResponseDTO;
import com.uteq.backend.repository.StatusBookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catálogo estados_libro (FIX 3): los <select> del formulario de libros
// necesitan listar estados. Mismo patrón que CategoriaController/
// AutorController (GET sin @PreAuthorize, array plano sin paginación).
@RestController
@RequestMapping("/api/v1/estados-libro")
public class StatusBookController {

    private final StatusBookRepository statusBookRepository;

    /**
     * Constructor con el repositorio de estados del libro.
     *
     * @param statusBookRepository repositorio del catálogo de estados del libro
     */
    public StatusBookController(StatusBookRepository statusBookRepository) {
        this.statusBookRepository = statusBookRepository;
    }
    /**
     * Lista todos los estados del libro para los selectores del formulario de libros.
     *
     * @return lista completa de estados con id y nombre
     */
    @GetMapping
    public ResponseEntity<List<StatusBookResponseDTO>> list() {
        List<StatusBookResponseDTO> statuses = statusBookRepository.findAll().stream()
                .map(e -> new StatusBookResponseDTO(e.getId(), e.getName()))
                .toList();
        return ResponseEntity.ok(statuses);
    }
}