package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

// Expone tipoNotificacionId como identificador plano (no el nombre
// resuelto del catálogo), mismo criterio que MultaResponseDTO.estadoMultaId
// -- MultaService.toDTO() tampoco resuelve el nombre del estado, para no
// pagar un join/consulta extra solo por legibilidad en un listado paginado.
public record NotificationResponseDTO(
        // id = notificación.id; prestamoId = préstamo real (antes llevaba
        // el id de la notificación y el "Préstamo #" mostraba otro número).
        @JsonProperty("id") Long id, @JsonProperty("prestamoId") Long loanId, @JsonProperty("tipoNotificacionId") Integer typeNotificationId, @JsonProperty("mensaje") String message, @JsonProperty("fechaEnvio") OffsetDateTime dateEnvio,
        boolean enviadoOk,
        @JsonProperty("creadoEn") OffsetDateTime created
) {
}
