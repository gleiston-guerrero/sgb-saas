package com.uteq.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Categoría de daño (categorias_dano).
 *
 * @param id identificador de la categoría de daño
 * @param name nombre de la categoría de daño
 */
public record CategoryDamageDTO( Integer id, @JsonProperty("nombre") String name) {}
