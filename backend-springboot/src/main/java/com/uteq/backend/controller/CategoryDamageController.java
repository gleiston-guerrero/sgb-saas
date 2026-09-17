package com.uteq.backend.controller;

import com.uteq.backend.dto.CategoryDamageDTO;
import com.uteq.backend.entity.CategoryDamage;
import com.uteq.backend.repository.CategoryDamageRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias-dano")
/**
 * Catálogo de categorías de daño (gestión ADMIN, lectura extendida).
 */
public class CategoryDamageController {

    private final CategoryDamageRepository repo;

    /**
     * Constructor con el repositorio de categorías de daño.
     *
     * @param repo repositorio del catálogo de categorías de daño
     */
    public CategoryDamageController(CategoryDamageRepository repo) {
        this.repo = repo;
    }
    /**
     * Lista las categorías de daño registradas. Roles BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @return lista de categorías de daño con id y nombre
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<CategoryDamageDTO>> list() {
        return ResponseEntity.ok(repo.findAll().stream().map(c -> new CategoryDamageDTO(c.getId(), c.getName())).toList());
    }
    /**
     * Crea una categoría de daño si el nombre no existe. Solo ADMIN.
     *
     * @param req nombre de la categoría de daño a registrar
     * @return categoría creada con su id y cabecera de ubicación
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDamageDTO> create(@RequestBody CategoryRequest req) {
        if (req.name() == null || req.name().isBlank()) return ResponseEntity.badRequest().build();
        if (repo.findByName(req.name()).isPresent()) return ResponseEntity.unprocessableEntity().build();
        CategoryDamage c = new CategoryDamage();
        c.setName(req.name().trim());
        CategoryDamage g = repo.save(c);
        return ResponseEntity.created(URI.create("/api/v1/categorias-dano/" + g.getId())).body(new CategoryDamageDTO(g.getId(), g.getName()));
    }
    /**
     * Cambia el nombre de una categoría de daño existente. Solo ADMIN.
     *
     * @param id id de la categoría de daño a actualizar
     * @param req nombre nuevo de la categoría de daño
     * @return categoría actualizada, o 404 si no existe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDamageDTO> update(@PathVariable Integer id, @RequestBody CategoryRequest req) {
        CategoryDamage c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setName(req.name().trim());
        CategoryDamage g = repo.save(c);
        return ResponseEntity.ok(new CategoryDamageDTO(g.getId(), g.getName()));
    }
    /**
     * Desactiva una categoría de daño sin borrar su registro. Solo ADMIN.
     *
     * @param id id de la categoría de daño a desactivar
     * @return respuesta vacía con estado 204, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        CategoryDamage c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setActive(false);
        repo.save(c);
        return ResponseEntity.noContent().build();
    }
    /**
     * Cuerpo de creación y edición de una categoría de daño.
     *
     * @param name nombre de la categoría de daño
     */

    public record CategoryRequest(@NotBlank @JsonProperty("nombre") String name) {}
}
