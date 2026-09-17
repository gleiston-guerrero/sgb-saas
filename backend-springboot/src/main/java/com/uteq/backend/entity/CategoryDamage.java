package com.uteq.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Categoría de daño del catálogo (tabla categorias_dano).
 */
@Data
@Entity
@Table(name = "categorias_dano")
public class CategoryDamage {
    /**
     * Constructor sin argumentos para JPA y Jackson.
     */
    public CategoryDamage() {
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonProperty("nombre")
    @Column(name = "nombre", nullable = false, unique = true, length = 50)  private String name;

    @Column(name = "activo", nullable = false)
    private Boolean active = true;
}
