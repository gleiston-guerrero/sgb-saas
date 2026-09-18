package com.uteq.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Deserializador Jackson que acepta fechas ISO flexibles con o sin segundos,
 * milisegundos y offset. Si falta el offset usa {@code -05:00} de América/Guayaquil
 * y normaliza el espacio como separador {@code T}.
 */
public class FlexibleOffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.ofHours(-5); // America/Guayaquil
    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .optionalStart().appendPattern(".SSS").optionalEnd()
            .optionalStart().appendOffsetId().optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .parseDefaulting(ChronoField.OFFSET_SECONDS, DEFAULT_OFFSET.getTotalSeconds())
            .toFormatter();

    /**
     * Crea el deserializador para {@code OffsetDateTime}.
     */
    public FlexibleOffsetDateTimeDeserializer() { super(OffsetDateTime.class); }
    /**
     * Convierte el texto JSON a {@code OffsetDateTime} con el formato flexible.
     * Devuelve null si el texto está vacío y usa {@code LocalDateTime} como respaldo.
     *
     * @param p parser JSON posicionado en el valor de fecha
     * @param ctxt contexto de deserialización de Jackson
     * @return fecha con offset interpretada, o null si el texto está vacío
     * @throws IOException si el texto no tiene un formato de fecha soportado
     */
    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        // Normaliza: si viene con espacio en vez de T, corrige
        text = text.replace(' ', 'T');
        try {
            return OffsetDateTime.parse(text, FMT);
        } catch (Exception e) {
            // Fallback: intenta LocalDateTime
            try {
                LocalDateTime ldt = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return ldt.atOffset(DEFAULT_OFFSET);
            } catch (Exception ex) {
                throw new IOException("Formato de fecha no soportado: " + text, ex);
            }
        }
    }
}
