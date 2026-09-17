package com.uteq.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Autor del catálogo (autores).
 *
 * @param id identificador del autor
 * @param name nombre del autor
 */
public record AuthorResponseDTO(
        Long id, @JsonProperty("nombre") String name
) {}
