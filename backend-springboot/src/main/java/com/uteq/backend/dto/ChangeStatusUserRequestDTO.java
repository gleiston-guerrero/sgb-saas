package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code PATCH /api/v1/admin/usuarios/{id}/estado}. El motivo es obligatorio para dejar rastro auditable.
 *
 * @param freshStatus nuevo estado (nombre del catálogo estados_usuario)
 * @param reason motivo del cambio, se persiste en usuario_motivos_cambio
 */
public record ChangeStatusUserRequestDTO(
        @NotBlank(message = "El nuevo estado es obligatorio") @JsonProperty("nuevoEstado") String freshStatus,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        @JsonProperty("motivo") String reason
) {
}
