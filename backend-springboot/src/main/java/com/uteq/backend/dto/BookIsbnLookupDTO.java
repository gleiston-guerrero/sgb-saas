package com.uteq.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;


// Respuesta de GET /api/v1/libros/lookup-isbn?isbn= (Google Books).
// anioPublicacion puede ser null si Google Books no trae fecha; la
// portada NO viaja acá (se descarga aparte por /lookup-isbn/portada).
/**
 * Autocompletado de libro desde Google Books por ISBN.
 *
 * @param title título encontrado
 * @param author autor encontrado
 * @param summary resumen encontrado
 * @param yearPublication año de publicación, null si no hay fecha
 * @param coverAvailable si hay portada descargable
 * @param publisher editorial encontrada
 * @param numberPages número de páginas
 */
public record BookIsbnLookupDTO( @JsonProperty("titulo") String title, @JsonProperty("autor") String author, @JsonProperty("resumen") String summary, @JsonProperty("anioPublicacion") Integer yearPublication, @JsonProperty("portadaDisponible") Boolean coverAvailable, @JsonProperty("editorial") String publisher,
        @JsonProperty("numeroPaginas") Integer numberPages
) {}