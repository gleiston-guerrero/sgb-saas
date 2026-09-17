package com.uteq.backend.chatbot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uteq.backend.entity.StatusReservation;
import com.uteq.backend.entity.Book;
import com.uteq.backend.repository.StatusReservationRepository;
import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.ReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tool que consulta las reservas vigentes de un usuario.
 * "Vigente" = estado PENDIENTE o LISTA_PARA_RETIRO.
 * El usuario_id se inyecta automáticamente desde el orchestrator.
 */
@Component
public class QueryReservationsTool extends AbstractUserAwareTool {

    private final ReservationRepository reservationRepo;
    private final StatusReservationRepository statusReservationRepo;
    private final BookRepository bookRepo;

    /**
     * Crea la tool con los repositorios de reservaciones, estados y libros.
     *
     * @param reservationRepo repositorio para paginar reservaciones por usuario
     * @param statusReservationRepo repositorio para resolver los estados vigentes y sus nombres
     * @param bookRepo repositorio para enriquecer cada reserva con título e ISBN
     */
    public QueryReservationsTool(ReservationRepository reservationRepo,
                                      StatusReservationRepository statusReservationRepo,
                                      BookRepository bookRepo) {
        this.reservationRepo = reservationRepo;
        this.statusReservationRepo = statusReservationRepo;
        this.bookRepo = bookRepo;
    }
    /**
     * Devuelve el nombre único que Gemini usa para invocar esta tool.
     *
     * @return nombre {@code consultar_reservaciones}
     */
    @Override
    public String getName() {
        return "consultar_reservaciones";
    }
    /**
     * Describe que esta tool expone las reservas vigentes ({@code PENDIENTE} o
     * {@code LISTA_PARA_RETIRO}) de un usuario. Recibe {@code usuario_id} y devuelve
     * un JSON con el arreglo {@code reservas_vigentes} (libro, fechas y estado) y su {@code total}.
     *
     * @return descripción legible por Gemini para decidir cuándo invocar la tool
     */
    @Override
    public String getDescription() {
        return "Consulta las reservas vigentes (PENDIENTE o LISTA_PARA_RETIRO) de un usuario de la biblioteca. "
                + "Devuelve el listado con libro, fechas y estado.";
    }
    /**
     * Consulta la primera página de reservaciones del usuario, filtra las vigentes por estado
     * y las enriquece con los datos del libro y el nombre del estado.
     *
     * @param args nodo JSON con {@code usuario_id} inyectado desde la sesión autenticada
     * @return nodo JSON con el arreglo de reservas vigentes, el total y el usuario, o error si faltan datos
     */
    @Override
    public JsonNode execute(JsonNode args) {
        Long userId = resolveUserId(args);
        if (userId == null) {
            return errorNode("Se requiere usuario_id");
        }

        // Resolver IDs de estados vigentes
        Integer statusPendingId = statusReservationRepo.findByName("PENDIENTE")
                .map(StatusReservation::getId)
                .orElse(null);
        Integer statusListaForPickupId = statusReservationRepo.findByName("LISTA_PARA_RETIRO")
                .map(StatusReservation::getId)
                .orElse(null);

        if (statusPendingId == null && statusListaForPickupId == null) {
            return errorNode("Estados de reserva vigentes no encontrados en catálogo");
        }

        Page<com.uteq.backend.entity.Reservation> page = reservationRepo.findByUserId(
                userId, PageRequest.of(0, 20));

        List<com.uteq.backend.entity.Reservation> vigentes = page.getContent().stream()
                .filter(r -> (statusPendingId != null && statusPendingId.equals(r.getStatusReservationId()))
                        || (statusListaForPickupId != null && statusListaForPickupId.equals(r.getStatusReservationId())))
                .toList();

        ArrayNode reservationsArray = mapper.createArrayNode();
        for (com.uteq.backend.entity.Reservation r : vigentes) {
            Optional<Book> bookOpt = bookRepo.findById(r.getBookId());
            Optional<StatusReservation> statusOpt = statusReservationRepo.findById(r.getStatusReservationId());

            if (bookOpt.isEmpty() || statusOpt.isEmpty()) {
                continue; // saltar si falta dato relacionado
            }

            Book book = bookOpt.get();
            StatusReservation status = statusOpt.get();

            ObjectNode node = mapper.createObjectNode();
            node.put("reservacion_id", r.getId());
            node.put("libro_id", book.getId());
            node.put("titulo", book.getTitle());
            node.put("isbn", book.getIsbn());
            node.put("fecha_reserva", r.getDateReservation() != null ? r.getDateReservation().toString() : null);
            node.put("fecha_limite_retiro", r.getDateLimitPickup() != null ? r.getDateLimitPickup().toString() : null);
            node.put("estado", status.getName());
            reservationsArray.add(node);
        }

        ObjectNode response = mapper.createObjectNode();
        response.set("reservas_vigentes", reservationsArray);
        response.put("total", vigentes.size());
        response.put(USUARIO_ID, userId);
        return response;
    }
}
