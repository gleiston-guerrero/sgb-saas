package com.uteq.backend.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry que recolecta todas las {@link ChatbotTool} beans y las expone
 * en dos formatos:
 * <ul>
 *   <li>{@link #buildToolsPayload} — formato Gemini (para el campo {@code tools} del payload)</li>
 *   <li>{@link #execute(String, JsonNode)} — ejecuta una tool por nombre y devuelve el resultado</li>
 * </ul>
 */
@Component
public class ChatbotToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChatbotToolRegistry.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, ChatbotTool> tools;

    /**
     * Crea el registro a partir de las tools detectadas por Spring.
     *
     * @param toolList lista de tools inyectada por Spring
     */
    public ChatbotToolRegistry(List<ChatbotTool> toolList) {
        this.tools = new LinkedHashMap<>();
        for (ChatbotTool tool : toolList) {
            this.tools.put(tool.getName(), tool);
            log.info("Chatbot tool registrada: {} — {}", tool.getName(), tool.getDescription());
        }
        log.info("Total tools disponibles: {}", tools.size());
    }

    /**
     * Construye el payload de declaraciones de función en formato Gemini.
     * Cada tool aporta su nombre, su descripción y su schema de parámetros.
     *
     * @return lista con el mapa {@code functionDeclarations} para el campo {@code tools}
     */
    public List<Map<String, Object>> buildToolsPayload() {
        ArrayNode functionDeclarations = MAPPER.createArrayNode();

        for (ChatbotTool tool : tools.values()) {
            ObjectNode declaration = MAPPER.createObjectNode();
            declaration.put("name", tool.getName());
            declaration.put("description", tool.getDescription());
            declaration.set("parameters", tool.getInputSchema());
            functionDeclarations.add(declaration);
        }

        Map<String, Object> functionDeclarationsWrapper = new LinkedHashMap<>();
        functionDeclarationsWrapper.put("functionDeclarations", functionDeclarations);

        return List.of(functionDeclarationsWrapper);
    }

    /**
     * Ejecuta la tool indicada por nombre con los argumentos de Gemini.
     * Si la tool no existe o falla, devuelve un nodo JSON con la clave {@code error}.
     *
     * @param toolName nombre de la tool solicitada por Gemini
     * @param args argumentos JSON del {@code functionCall}
     * @return resultado JSON de la tool o nodo de error
     */
    public JsonNode execute(String toolName, JsonNode args) {
        ChatbotTool tool = tools.get(toolName);
        if (tool == null) {
            log.warn("Tool desconocida solicitada por Gemini: {}", toolName);
            return MAPPER.createObjectNode().put("error", "Tool no encontrada: " + toolName);
        }
        try {
            return tool.execute(args);
        } catch (Exception ex) {
            log.error("Error ejecutando tool {}: {}", toolName, ex.getMessage(), ex);
            return MAPPER.createObjectNode().put("error", "Error ejecutando " + toolName + ": " + ex.getMessage());
        }
    }

    /**
     * Indica si existe una tool registrada con ese nombre.
     *
     * @param toolName nombre de la tool a buscar
     * @return true si la tool está registrada; false en caso contrario
     */

    public boolean contains(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Devuelve el schema JSON de entrada de una tool.
     * Si la tool no existe, devuelve un objeto vacío.
     *
     * @param toolName nombre de la tool a consultar
     * @return schema de entrada como nodo JSON
     */
    public JsonNode getToolSchema(String toolName) {
        ChatbotTool tool = tools.get(toolName);
        if (tool == null) {
            return MAPPER.createObjectNode();
        }
        return tool.getInputSchema();
    }

    /**
     * Indica si el schema de la tool exige {@code usuario_id} en su arreglo {@code required}.
     *
     * @param toolName nombre de la tool a consultar
     * @return true si requiere {@code usuario_id}; false en caso contrario
     */
    public boolean requiresUserId(String toolName) {
        JsonNode schema = getToolSchema(toolName);
        JsonNode required = schema.path("required");
        if (!required.isArray()) {
            return false;
        }
        for (JsonNode req : required) {
            if ("usuario_id".equals(req.asText())) {
                return true;
            }
        }
        return false;
    }
}
