package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.repository.LoanRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool que consulta los préstamos activos de un usuario específico.
 * Usa el repository directamente (sin Authentication) porque la tool ya
 * fue invocada en contexto autenticado (ChatbotOrchestrator validó el usuario).
 */
@Component
public class QueryLoansTool extends AbstractUserAwareTool {

    private final LoanRepository loanRepo;

    /**
     * Crea la tool con el repositorio de préstamos.
     *
     * @param loanRepo repositorio para buscar préstamos activos por usuario
     */
    public QueryLoansTool(LoanRepository loanRepo) {
        this.loanRepo = loanRepo;
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code consultar_prestamos}
     */
    @Override
    public String getName() {
        return "consultar_prestamos";
    }
    /**
     * Describe que esta tool expone los préstamos activos (no devueltos) de un usuario.
     * Recibe {@code usuario_id} y devuelve un JSON con el arreglo {@code prestamos_activos}
     * (título, ISBN, fechas, días restantes y estado) y su {@code total}.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Consulta los préstamos activos (no devueltos) de un usuario de la biblioteca. "
                + "Devuelve títulos, ISBNs, fechas de préstamo y devolución estimada.";
    }
    /**
     * Consulta los préstamos activos del usuario y calcula los días restantes
     * con la fecha de devolución estimada de cada préstamo.
     *
     * @param args nodo JSON con {@code usuario_id} inyectado desde la sesión autenticada
     * @return nodo JSON con el arreglo de préstamos, el total y el usuario, o error si falta el usuario
     */
    @Override
    public JsonNode execute(JsonNode args) {
        Long userId = resolveUserId(args);
        if (userId == null) {
            return errorNode("Se requiere usuario_id");
        }

        List<com.uteq.backend.repository.projection.LoanActiveBaseProjection> loans =
                loanRepo.findActivesByUserId(userId);

        ArrayNode loansArray = mapper.createArrayNode();
        for (com.uteq.backend.repository.projection.LoanActiveBaseProjection p : loans) {
            ObjectNode node = mapper.createObjectNode();
            node.put("prestamo_id", p.getLoanId());
            node.put("titulo", p.getBookTitle());
            node.put("isbn", p.getBookIsbn());
            node.put("fecha_prestamo", p.getDateLoan() != null ? p.getDateLoan().toInstant().toString() : null);
            node.put("fecha_devolucion_estimada", p.getDateLoanReturnEstimada() != null ? p.getDateLoanReturnEstimada().toInstant().toString() : null);
            Integer dias = com.uteq.backend.service.LoanService.diasRestantes(p.getDateLoanReturnEstimada() != null
                    ? p.getDateLoanReturnEstimada().toInstant() : null);
            if (dias != null) {
                node.put("dias_restantes", dias.intValue());
            } else {
                node.putNull("dias_restantes");
            }
            node.put("estado", p.getStatusName());
            loansArray.add(node);
        }

        ObjectNode response = mapper.createObjectNode();
        response.set("prestamos_activos", loansArray);
        response.put("total", loans.size());
        response.put(USUARIO_ID, userId);
        return response;
    }
}
