package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record SuggestionAcquisitionResponseDTO(
        // id = sugerencia.id (el track por id y el PATCH de estado lo usan).
        // usuarioId = usuario real: antes llevaba el id de la sugerencia.
        @JsonProperty("id") Long id, @JsonProperty("usuarioId") Long userId, @JsonProperty("titulo") String title, @JsonProperty("autor") String author,
        String isbn,
        String justificacion, @JsonProperty("estado") String status, @JsonProperty("revisadoPor") Long revisadoBy, @JsonProperty("creadoEn") OffsetDateTime created
) {}
