package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.repository.FineRepository;
import com.uteq.backend.repository.StatusFineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Tool que consulta las multas pendientes de pago de un usuario.
 * Incluye saldo total adeudado y cantidad de multas.
 */
@Component
public class QueryFinesTool extends AbstractUserAwareTool {

    private final FineRepository fineRepo;
    private final StatusFineRepository statusFineRepo;

    /**
     * Crea la tool con los repositorios de multas y de estados de multa.
     *
     * @param fineRepo repositorio para contar y sumar saldos de multas por usuario y estado
     * @param statusFineRepo repositorio para resolver el estado {@code PENDIENTE}
     */
    public QueryFinesTool(FineRepository fineRepo, StatusFineRepository statusFineRepo) {
        this.fineRepo = fineRepo;
        this.statusFineRepo = statusFineRepo;
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code consultar_multas}
     */
    @Override
    public String getName() {
        return "consultar_multas";
    }
    /**
     * Describe que esta tool expone las multas pendientes de un usuario.
     * Recibe {@code usuario_id} y devuelve un JSON con {@code multas_pendientes},
     * {@code saldo_total_pendiente} y {@code tiene_multas_pendientes}.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Consulta las multas pendientes de pago de un usuario de la biblioteca. "
                + "Devuelve el saldo total adeudado y la cantidad de multas pendientes.";
    }
    /**
     * Consulta las multas en estado {@code PENDIENTE} del usuario: cuenta cuántas hay
     * y suma sus saldos para el total adeudado.
     *
     * @param args nodo JSON con {@code usuario_id} inyectado desde la sesión autenticada
     * @return nodo JSON con el conteo, el saldo total y el indicador de deuda, o error si falta el usuario o el catálogo
     */
    @Override
    public JsonNode execute(JsonNode args) {
        Long userId = resolveUserId(args);
        if (userId == null) {
            return errorNode("Se requiere usuario_id");
        }

        Integer statusPendingId = statusFineRepo.findByName("PENDIENTE")
                .map(e -> e.getId())
                .orElse(null);

        if (statusPendingId == null) {
            return errorNode("Estado PENDIENTE no encontrado en catálogo");
        }

        long quantity = fineRepo.countByUserIdAndStatusFineId(userId, statusPendingId);
        BigDecimal balanceTotal = fineRepo.sumBalanceByUserIdAndStatusFineId(userId, statusPendingId);

        ObjectNode response = mapper.createObjectNode();
        response.put(USUARIO_ID, userId);
        response.put("multas_pendientes", quantity);
        response.put("saldo_total_pendiente", balanceTotal != null ? balanceTotal.doubleValue() : 0.0);
        response.put("tiene_multas_pendientes", quantity > 0);
        return response;
    }
}
