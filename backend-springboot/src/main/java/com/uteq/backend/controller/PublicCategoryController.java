package com.uteq.backend.controller;

import com.uteq.backend.dto.CategoryResponseDTO;
import com.uteq.backend.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publico/categorias")
public class PublicCategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Constructor con el repositorio de categorías.
     *
     * @param categoryRepository repositorio del catálogo de categorías
     */
    public PublicCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    /**
     * Lista todas las categorías para el portal público sin autenticación.
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
}
