package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.chatbot.ChatbotTool;

/**
 * Base abstracta para TODAS las tools del chatbot.
 * Centraliza el {@link ObjectMapper} compartido y el helper de
 * nodos de error (antes duplicados en cada tool concreta).
 * No lleva {@code @Component}: Spring solo registra las subclases
 * concretas vía {@code List<ChatbotTool>} en el registry.
 */
public abstract class AbstractChatbotTool implements ChatbotTool {

    /** ObjectMapper compartido por todas las tools para armar schemas y errores. */
    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor sin argumentos para las subclases concretas.
     */
    protected AbstractChatbotTool() {
    }

    /**
     * Arma un nodo de error con el mensaje dado.
     *
     * @param message mensaje de error a incluir en el nodo
     * @return nodo JSON con el error
     */
    protected ObjectNode errorNode(String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put("error", message);
        return error;
    }
}
