package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Tipo de daño del catálogo (tipos_dano).
 *
 * @param id identificador del tipo de daño
 * @param name nombre del tipo de daño
 * @param categoryId identificador de la categoría de daño
 * @param categoryName nombre de la categoría de daño
 * @param typeCost tipo de costo (FIJO o PORCENTAJE)
 * @param value valor del costo
 */
public record TypeDamageDTO(
        Integer id, @JsonProperty("nombre") String name, @JsonProperty("categoriaId") Integer categoryId, @JsonProperty("categoriaNombre") String categoryName, @JsonProperty("tipoCosto") String typeCost, @JsonProperty("valor") BigDecimal value
) {
    /**
     * Handles precio.
     *
     * @return big decimal with the resulting state after the operation
     */
    public BigDecimal price() { return value; }
}
