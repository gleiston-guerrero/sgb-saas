package com.uteq.backend.controller;

import com.uteq.backend.dto.TypeDamageDTO;
import com.uteq.backend.service.TypeDamageService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-dano")
@PreAuthorize("hasRole('ADMIN')")
public class TypeDamageController {

    private final TypeDamageService typeDamageService;

    /**
     * Constructor con el servicio de tipos de daño.
     *
     * @param typeDamageService servicio del catálogo de tipos de daño
     */
    public TypeDamageController(TypeDamageService typeDamageService) {
        this.typeDamageService = typeDamageService;
    }
    /**
     * Lista los tipos de daño con su categoría y costo. Lectura para BIBLIOTECARIO, GERENTE y ADMIN.
     *
     * @return lista de tipos de daño registrados
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','GERENTE','ADMIN')")
    public ResponseEntity<List<TypeDamageDTO>> list() {
        return ResponseEntity.ok(typeDamageService.listAll());
    }
    /**
     * Crea un tipo de daño con su categoría y regla de costo. Solo ADMIN.
     *
     * @param dto nombre, categoría, tipo de costo y valor del daño
     * @return tipo de daño creado con su id y cabecera de ubicación
     */
    @PostMapping
    public ResponseEntity<TypeDamageDTO> create(@Valid @RequestBody TypeDamageRequestDTO dto) {
        TypeDamageDTO created = typeDamageService.create(dto.name(), dto.categoryId(), dto.typeCost(), dto.value());
        return ResponseEntity.created(URI.create("/api/v1/tipos-dano/" + created.id())).body(created);
    }
    /**
     * Actualiza el nombre, categoría y costo de un tipo de daño existente. Solo ADMIN.
     *
     * @param id id del tipo de daño a actualizar
     * @param dto nombre, categoría, tipo de costo y valor nuevos
     * @return tipo de daño actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<TypeDamageDTO> update(@PathVariable Integer id, @Valid @RequestBody TypeDamageRequestDTO dto) {
        return ResponseEntity.ok(typeDamageService.update(id, dto.name(), dto.categoryId(), dto.typeCost(), dto.value()));
    }
    /**
     * Elimina un tipo de daño por su id. Solo ADMIN.
     *
     * @param id id del tipo de daño a eliminar
     * @return respuesta vacía con estado 204 si se eliminó
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        typeDamageService.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Cuerpo de creación y edición de un tipo de daño con su regla de costo.
     *
     * @param name nombre del tipo de daño
     * @param categoryId id de la categoría de daño a la que pertenece
     * @param typeCost tipo de costo aplicado al daño
     * @param value valor del costo del daño
     */

    public record TypeDamageRequestDTO(
            @NotBlank @JsonProperty("nombre") String name,
            @NotNull @JsonProperty("categoriaId") Integer categoryId,
            @NotBlank @JsonProperty("tipoCosto") String typeCost,
            @NotNull @DecimalMin("0") @JsonProperty("valor") BigDecimal value
    ) {}
}
