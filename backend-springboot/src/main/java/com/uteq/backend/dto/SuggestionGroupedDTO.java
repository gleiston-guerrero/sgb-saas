package com.uteq.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;


// Fila agregada de sugerencias PENDIENTE por ISBN (gestión por demanda:
// lo más pedido se adquiere primero). titulo/autor son MAX() del grupo
// porque el mismo ISBN puede venir con variantes de tipeo.
public record SuggestionGroupedDTO(
        // isbn con su nombre: el refactor lo había aliasado como titulo y
        // gestión mostraba el ISBN como título + confirmaba con undefined.
        @JsonProperty("isbn") String isbn, @JsonProperty("titulo") String title, @JsonProperty("autor") String author, @JsonProperty("cantidad") Long quantity
) {}
