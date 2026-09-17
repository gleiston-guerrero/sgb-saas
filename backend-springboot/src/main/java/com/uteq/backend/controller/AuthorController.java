package com.uteq.backend.controller;

import com.uteq.backend.dto.AuthorRequestDTO;
import com.uteq.backend.dto.AuthorResponseDTO;
import com.uteq.backend.entity.Author;
import com.uteq.backend.repository.AuthorRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Catálogo de autores (lectura pública autenticada, gestión GERENTE/ADMIN).
 */
@RestController
@RequestMapping("/api/v1/autores")
public class AuthorController {

    private final AuthorRepository authorRepository;

    /**
     * Constructor con el repositorio de autores.
     *
     * @param authorRepository repositorio del catálogo de autores
     */
    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * Lists author.
     *
     * @return response entity{@code <list<autor response dto>>} with the resulting state after the operation
     */
    @GetMapping
    public ResponseEntity<List<AuthorResponseDTO>> list() {
        List<AuthorResponseDTO> authors = authorRepository.findAll().stream()
                .map(a -> new AuthorResponseDTO(a.getId(), a.getName()))
                .toList();
        return ResponseEntity.ok(authors);
    }

    /**
     * Consulta search usando los filtros recibidos y devuelve el resultado solicitado.
     *
     * @param q texto de busqueda o filtro usado para reducir los resultados devueltos
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<AuthorResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                authorRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(a -> new AuthorResponseDTO(a.getId(), a.getName()))
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
    public ResponseEntity<AuthorResponseDTO> create(@Valid @RequestBody AuthorRequestDTO dto) {
        Author author = new Author();
        author.setName(dto.name());
        Author guardado = authorRepository.save(author);
        return ResponseEntity.created(URI.create("/api/v1/autores/" + guardado.getId()))
                .body(new AuthorResponseDTO(guardado.getId(), guardado.getName()));
    }
}
