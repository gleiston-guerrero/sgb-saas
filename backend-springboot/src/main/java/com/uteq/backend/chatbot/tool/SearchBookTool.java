package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.dto.BookSuggestionDTO;
import com.uteq.backend.service.BookService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool que busca libros en el catálogo real por título, autor o tema.
 * Usa {@link BookService#suggest(String)} que internamente consulta con
 * pg_trgm (similitud de texto) y retorna los 3 resultados más relevantes.
 */
@Component
public class SearchBookTool extends AbstractChatbotTool {

    private static final String PARAM_QUERY = "query";

    private final BookService bookService;

    /**
     * Crea la tool con el servicio de libros para sugerencias del catálogo.
     *
     * @param bookService servicio que busca por similitud de texto en el catálogo
     */
    public SearchBookTool(BookService bookService) {
        this.bookService = bookService;
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code buscar_libro}
     */
    @Override
    public String getName() {
        return "buscar_libro";
    }
    /**
     * Describe que esta tool busca libros del catálogo por título, autor o tema.
     * Recibe {@code query} y devuelve un JSON con el arreglo {@code resultados}
     * (id, título y disponibilidad), su {@code total} y el {@code query} usado.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Busca libros en el catálogo de la biblioteca por título, autor o tema. "
                + "Devuelve los resultados más relevantes con su disponibilidad actual.";
    }
    /**
     * Devuelve el schema de entrada: objeto que exige {@code query} con el título, autor o tema a buscar.
     *
     * @return schema JSON con la propiedad {@code query} requerida
     */
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        ObjectNode queryProp = mapper.createObjectNode();
        queryProp.put("type", "string");
        queryProp.put("description", "Título, autor o tema a buscar (ej: 'Clean Code', 'machine learning')");
        properties.set(PARAM_QUERY, queryProp);

        schema.set("properties", properties);

        ArrayNode required = mapper.createArrayNode();
        required.add(PARAM_QUERY);
        schema.set("required", required);

        return schema;
    }
    /**
     * Busca en el catálogo con el texto recibido y mapea las sugerencias a id, título y disponibilidad.
     *
     * @param args nodo JSON con {@code query} (título, autor o tema a buscar)
     * @return nodo JSON con los resultados, el total y el query usado
     */
    @Override
    public JsonNode execute(JsonNode args) {
        String query = args.path(PARAM_QUERY).asText("");
        List<BookSuggestionDTO> results = bookService.suggest(query);

        ArrayNode resultsArray = mapper.createArrayNode();
        for (BookSuggestionDTO book : results) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", book.id());
            node.put("titulo", book.title());
            node.put("disponible", Boolean.TRUE.equals(book.available()));
            resultsArray.add(node);
        }

        ObjectNode response = mapper.createObjectNode();
        response.set("resultados", resultsArray);
        response.put("total", results.size());
        response.put("query", query);
        return response;
    }
}
