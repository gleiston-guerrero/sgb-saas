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
     * Lists category.
     *
     * @return response entity{@code <list<categoria response dto>>} with the resulting state after the operation
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> list() {
        List<CategoryResponseDTO> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(categories);
    }
    /**
     * Consulta search usando los filtros recibidos y devuelve el resultado solicitado.
     *
     * @param q texto de busqueda o filtro usado para reducir los resultados devueltos
     * @return respuesta HTTP con el estado y el cuerpo definidos por la operacion
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CategoryResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                categoryRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(c -> new CategoryResponseDTO(c.getId(), c.getName()))
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
