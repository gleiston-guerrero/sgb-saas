package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.entity.KnowledgeBase;
import com.uteq.backend.repository.BaseKnowledgeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool que consulta información sobre políticas de la biblioteca:
 * préstamo, devolución, sanciones, etc.
 * Filtra la base de conocimiento por categorías POLITICAS y MULTAS.
 */
@Component
public class InfoPoliciesTool extends AbstractKnowledgeBaseTool {

    /**
     * Crea la tool con el repositorio de la base de conocimiento.
     *
     * @param baseKnowledgeRepo repositorio de entradas de conocimiento activas
     */
    public InfoPoliciesTool(BaseKnowledgeRepository baseKnowledgeRepo) {
        super(baseKnowledgeRepo);
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code info_politicas}
     */
    @Override
    public String getName() {
        return "info_politicas";
    }
    /**
     * Describe que esta tool expone las políticas de la biblioteca (préstamo, devolución,
     * renovaciones, sanciones y reglas generales). No recibe argumentos y devuelve
     * un JSON con el arreglo {@code politicas} y su {@code total}.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Consulta información sobre las políticas de la biblioteca: "
                + "préstamo, devolución, renovaciones, sanciones, multas y reglas generales.";
    }

    @Override
    protected List<String> getCategories() {
        return List.of("POLITICAS", "MULTAS");
    }

    @Override
    protected String getResponseKey() {
        return "politicas";
    }

    @Override
    protected ObjectNode mapInput(KnowledgeBase bc) {
        ObjectNode node = mapper.createObjectNode();
        node.put("categoria", bc.getCategory());
        node.put("pregunta", bc.getQuestionExample());
        node.put("respuesta", bc.getResponse());
        return node;
    }
}
