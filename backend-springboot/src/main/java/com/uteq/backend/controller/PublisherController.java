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

    public PublisherController(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }
    /**
     * Lists publisher.
     *
     * @return response entity{@code <list<editorial response dto>>} with the resulting state after the operation
     */
    @GetMapping
    public ResponseEntity<List<PublisherResponseDTO>> list() {
        List<PublisherResponseDTO> publishers = publisherRepository.findAll().stream()
                .map(e -> new PublisherResponseDTO(e.getId(), e.getName()))
                .toList();
        return ResponseEntity.ok(publishers);
    }
    /**
     * Consulta search usando los filtros recibidos y devuelve el resultado solicitado.
     *
     * @param q texto de busqueda o filtro usado para reducir los resultados devueltos
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<PublisherResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                publisherRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(e -> new PublisherResponseDTO(e.getId(), e.getName()))
                        .toList());
    }
    /**
     * Registra create validando los datos de entrada antes de persistir cambios.
     *
     * @param dto datos validados de la peticion con la informacion necesaria para ejecutar la operacion
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
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