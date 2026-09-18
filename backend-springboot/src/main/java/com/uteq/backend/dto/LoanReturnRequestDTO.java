package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body de POST /api/v1/prestamos/{id}/devolucion.
 * Contiene el estado de la devolución y opcionalmente los daños registrados.
 *
 * @param statusLoanReturn estado de la devolución
 * @param description descripción del estado del material
 * @param damages daños registrados en la devolución
 */
public record LoanReturnRequestDTO(

        @NotBlank(message = "El estado de devolucion es obligatorio") @JsonProperty("estadoDevolucion") String statusLoanReturn, @JsonProperty("descripcion") String description, @JsonProperty("danos") List<DamageItemDTO> damages
) {
    /**
     * Ítem de daño declarado en una devolución (tipo catalogado o
     * personalizado con precio cobrado).
     *
     * @param typeDamageId tipo de daño del catálogo, nulo si es personalizado
     * @param nameCustom nombre del daño personalizado, nulo si es catalogado
     * @param priceCobrado precio cobrado por el daño
     */
    public record DamageItemDTO(
            @JsonProperty("tipoDanoId") Integer typeDamageId,
            @JsonProperty("nombreCustom") String nameCustom,
            @JsonProperty("precioCobrado") BigDecimal priceCobrado
    ) {}
}
