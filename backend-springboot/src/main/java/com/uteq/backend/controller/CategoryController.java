package com.uteq.backend.controller;

import com.uteq.backend.dto.CategoryRequestDTO;
import com.uteq.backend.dto.CategoryResponseDTO;
import com.uteq.backend.entity.Category;
import com.uteq.backend.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Catálogo de categorías (lectura autenticada, gestión GERENTE/ADMIN).
 */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Constructor con el repositorio de categorías.
     *
     * @param categoryRepository repositorio del catálogo de categorías
     */
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    /**
     * Lista todas las categorías del catálogo para cualquier usuario autenticado.
     *
     * @return lista completa de categorías con id y nombre
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> list() {
        List<CategoryResponseDTO> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(categories);
    }
    /**
     * Busca hasta cinco categorías cuyo nombre contenga el texto dado, sin distinguir mayúsculas.
     *
     * @param q texto parcial del nombre de la categoría
     * @return lista de hasta cinco categorías coincidentes
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CategoryResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                categoryRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(c -> new CategoryResponseDTO(c.getId(), c.getName()))
                        .toList());
    }
    /**
     * Crea una categoría nueva si el nombre no existe. Solo GERENTE y ADMIN.
     *
     * @param dto nombre de la categoría a registrar
     * @return categoría creada con su id y cabecera de ubicación
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.name())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        Category category = new Category();
        category.setName(dto.name());
        Category guardada = categoryRepository.save(category);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + guardada.getId()))
                .body(new CategoryResponseDTO(guardada.getId(), guardada.getName()));
    }
}
