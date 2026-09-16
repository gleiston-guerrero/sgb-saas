package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FineResponseDTO(
        // id = multa.id (lo usan pago/anulación en el frontend). prestamoId
        // = préstamo real: el refactor a inglés había aliasado el id de la
        // multa como prestamoId y las acciones iban a /undefined/.
        @JsonProperty("id") Long id, @JsonProperty("prestamoId") Long loanId, @JsonProperty("monto") BigDecimal amount, @JsonProperty("estadoMultaId") Integer statusFineId, @JsonProperty("fechaGenerada") OffsetDateTime dateGenerated, @JsonProperty("fechaPagada") OffsetDateTime datePaid, @JsonProperty("observaciones") String observations
) {}