package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Detalle de un tipo de daño registrado en una devolución.
 *
 * @param id identificador del detalle
 * @param typeDamageName nombre del tipo de daño del catálogo
 * @param nameCustom nombre personalizado cuando el daño no está en el catálogo
 * @param priceCobrado monto cobrado por el daño
 */
public record DamageDetailResponseDTO(
        Long id, @JsonProperty("tipoDanoNombre") String typeDamageName, @JsonProperty("nombreCustom") String nameCustom, @JsonProperty("precioCobrado") BigDecimal priceCobrado
) {}
