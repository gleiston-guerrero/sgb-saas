package com.uteq.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Mapea la tabla "categorias".
/**
 * Categoría de libros (catálogo "categorias").
 */
@Data
@Entity
@Table(name = "categorias")
public class Category {

    /**
     * Constructor sin argumentos para JPA y Jackson.
     */
    public Category() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 80)
    @JsonProperty("nombre")
    @Column(name = "nombre", nullable = false, unique = true, length = 80)  private String name;
}
