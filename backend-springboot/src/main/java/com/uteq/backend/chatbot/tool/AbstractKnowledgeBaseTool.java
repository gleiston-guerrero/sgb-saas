package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.entity.KnowledgeBase;
import com.uteq.backend.repository.BaseKnowledgeRepository;

import java.util.List;

/**
 * Template Method para tools de solo-lectura sobre la base de
 * conocimiento (sin parámetros de entrada). Cada subclase aporta
 * las categorías a filtrar, la clave de respuesta y el mapeo
 * de cada entrada.
 */
public abstract class AbstractKnowledgeBaseTool extends AbstractChatbotTool {

    /** Repositorio de entradas de conocimiento activas. */
    protected final BaseKnowledgeRepository baseKnowledgeRepo;

    /**
     * Constructor con el repositorio de la base de conocimiento.
     *
     * @param baseKnowledgeRepo repositorio de entradas de conocimiento activas
     */
    protected AbstractKnowledgeBaseTool(BaseKnowledgeRepository baseKnowledgeRepo) {
        this.baseKnowledgeRepo = baseKnowledgeRepo;
    }
    /**
     * Retrieves input schema.
     *
     * @return json node with the resulting state after the operation
     */
    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", mapper.createObjectNode());
        return schema;
    }

    /**
     * Categorías de BaseConocimiento a incluir (ej. HORARIOS).
     *
     * @return lista de categorías que filtra esta tool
     */
    protected abstract List<String> getCategories();

    /**
     * Clave del array en la respuesta (ej. "horarios").
     *
     * @return clave bajo la que viaja el array de resultados
     */
    protected abstract String getResponseKey();

    /** Mapea una entrada de la base a su nodo JSON.
     *
     * @param bc entrada de la base de conocimiento a mapear
     * @return nodo JSON con la entrada mapeada
     */
    protected abstract ObjectNode mapInput(KnowledgeBase bc);
    /**
     * Procesa execute y devuelve el resultado calculado por el backend.
     *
     * @param args argumento recibido por la herramienta del chatbot para decidir y ejecutar la accion
     * @return objeto con el resultado de la operacion y los datos relevantes para el cliente
     */
    @Override
    public JsonNode execute(JsonNode args) {
        List<String> categories = getCategories().stream()
                .map(String::toUpperCase)
                .toList();
        List<KnowledgeBase> inputs = baseKnowledgeRepo.findByActiveTrue().stream()
                .filter(bc -> bc.getCategory() != null && categories.contains(bc.getCategory().toUpperCase()))
                .toList();

        ArrayNode array = mapper.createArrayNode();
        for (KnowledgeBase bc : inputs) {
            array.add(mapInput(bc));
        }

        ObjectNode response = mapper.createObjectNode();
        response.set(getResponseKey(), array);
        response.put("total", inputs.size());
        return response;
    }
}
