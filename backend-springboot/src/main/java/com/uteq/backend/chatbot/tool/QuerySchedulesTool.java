package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.entity.KnowledgeBase;
import com.uteq.backend.repository.BaseKnowledgeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool que consulta los horarios de apertura de la biblioteca.
 * Filtra la base de conocimiento por categoría HORARIOS.
 */
@Component
public class QuerySchedulesTool extends AbstractKnowledgeBaseTool {

    /**
     * Crea la tool con el repositorio de la base de conocimiento.
     *
     * @param baseKnowledgeRepo repositorio de entradas de conocimiento activas
     */
    public QuerySchedulesTool(BaseKnowledgeRepository baseKnowledgeRepo) {
        super(baseKnowledgeRepo);
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code consultar_horarios}
     */
    @Override
    public String getName() {
        return "consultar_horarios";
    }
    /**
     * Describe que esta tool expone los horarios de apertura (lunes a viernes, sábados y
     * días especiales). No recibe argumentos y devuelve un JSON con el arreglo
     * {@code horarios} y su {@code total}.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Consulta los horarios de apertura de la biblioteca. "
                + "Incluye horarios de lunes a viernes, sábados y días especiales.";
    }

    @Override
    protected List<String> getCategories() {
        return List.of("HORARIOS");
    }

    @Override
    protected String getResponseKey() {
        return "horarios";
    }

    @Override
    protected ObjectNode mapInput(KnowledgeBase bc) {
        ObjectNode node = mapper.createObjectNode();
        node.put("pregunta", bc.getQuestionExample());
        node.put("respuesta", bc.getResponse());
        return node;
    }
}
