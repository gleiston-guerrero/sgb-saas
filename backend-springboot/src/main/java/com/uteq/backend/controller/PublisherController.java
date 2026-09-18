package com.uteq.backend.controller;

import com.uteq.backend.dto.PublisherRequestDTO;
import com.uteq.backend.dto.PublisherResponseDTO;
import com.uteq.backend.entity.Publisher;
import com.uteq.backend.repository.PublisherRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Catálogo editoriales (FIX 3): los <select> del formulario de libros
// necesitan listar editoriales. Mismo patrón que CategoriaController/
// AutorController (GET sin @PreAuthorize, array plano sin paginación).
@RestController
@RequestMapping("/api/v1/editoriales")
public class PublisherController {

    private final PublisherRepository publisherRepository;

    /**
     * Constructor con el repositorio de editoriales.
     *
     * @param publisherRepository repositorio del catálogo de editoriales
     */
    public PublisherController(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }
    /**
     * Lista todas las editoriales del catálogo para cualquier usuario autenticado.
     *
     * @return lista completa de editoriales con id y nombre
     */
    @GetMapping
    public ResponseEntity<List<PublisherResponseDTO>> list() {
        List<PublisherResponseDTO> publishers = publisherRepository.findAll().stream()
                .map(e -> new PublisherResponseDTO(e.getId(), e.getName()))
                .toList();
        return ResponseEntity.ok(publishers);
    }
    /**
     * Busca hasta cinco editoriales cuyo nombre contenga el texto dado, sin distinguir mayúsculas.
     *
     * @param q texto parcial del nombre de la editorial
     * @return lista de hasta cinco editoriales coincidentes
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<PublisherResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                publisherRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(e -> new PublisherResponseDTO(e.getId(), e.getName()))
                        .toList());
    }
    /**
     * Crea una editorial nueva si el nombre no existe. Solo GERENTE y ADMIN.
     *
     * @param dto nombre de la editorial a registrar
     * @return editorial creada con su id y cabecera de ubicación
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<PublisherResponseDTO> create(@Valid @RequestBody PublisherRequestDTO dto) {
        if (publisherRepository.existsByNameIgnoreCase(dto.name())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        Publisher e = new Publisher();
        e.setName(dto.name());
        Publisher guardada = publisherRepository.save(e);
        return ResponseEntity.created(URI.create("/api/v1/editoriales/" + guardada.getId()))
                .body(new PublisherResponseDTO(guardada.getId(), guardada.getName()));
    }
}