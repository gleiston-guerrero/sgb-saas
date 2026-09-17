package com.uteq.backend.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uteq.backend.entity.Backup;
import com.uteq.backend.entity.BackupSchedule;
import com.uteq.backend.service.BackupScheduleService;
import com.uteq.backend.service.BackupService;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.uteq.backend.config.FlexibleOffsetDateTimeDeserializer;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Administración de respaldos (solo ADMIN): generar, listar, programar,
 * descargar y eliminar, más DTOs internos de request/respuesta.
 */
@RestController
@RequestMapping("/api/v1/admin/backups")
public class BackupController {

    private final BackupService backupService;
    private final BackupScheduleService progService;

    /**
     * Constructor con los servicios de respaldo y programación.
     *
     * @param backupService servicio de generación y descarga de respaldos
     * @param progService servicio de programaciones de respaldo
     */
    public BackupController(BackupService backupService, BackupScheduleService progService) {
        this.backupService = backupService;
        this.progService = progService;
    }

    /**
     * Genera un respaldo manual o automático con el rango de fechas, tablas y formato indicados.
     * Solo ADMIN. Devuelve el respaldo creado con su URL de descarga.
     *
     * @param req rango desde-hasta, tablas, formato y tipo de respaldo
     * @return respaldo creado con estado 201 y URL de descarga
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupResponseDTO> generate(@Valid @RequestBody BackupRequestDTO req) {
        String type = req.type != null ? req.type : "manual";
        Backup b = backupService.generateBackup(req.from, req.until, req.tables, req.format, type);
        String url = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}/download").buildAndExpand(b.getId()).toUriString();
        return ResponseEntity.status(HttpStatus.CREATED).body(new BackupResponseDTO(b.getId(), b.getCreated(), b.getFrom(), b.getUntil(), b.getTables(), b.getFormat(), b.getPath(), b.getSizeBytes(), b.getStatus(), b.getType(), url));
    }
    /**
     * Lista los respaldos generados, opcionalmente acotados por rango de fechas.
     * Solo ADMIN. Sin fechas lista todo; con fechas lista el rango indicado.
     *
     * @param from fecha inicial flexible del rango, null para listar todo
     * @param until fecha final flexible del rango, null para listar todo
     * @return lista resumida de respaldos del rango solicitado
     */

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BackupSummaryDTO>> list(@RequestParam(name = "desde", required = false) String from, @RequestParam(name = "hasta", required = false) String until) {
        OffsetDateTime d = from != null ? parseFlexible(from) : null;
        OffsetDateTime h = until != null ? parseFlexible(until) : null;
        OffsetDateTime ahora = OffsetDateTime.now();
        List<Backup> lista;
        if (d != null || h != null) {
            OffsetDateTime fromEffective = d != null ? d : ahora.minusDays(30);
            OffsetDateTime untilEffective = h != null ? h : ahora;
            lista = backupService.listByRange(fromEffective, untilEffective);
        } else {
            lista = backupService.listAll();
        }
        return ResponseEntity.ok(lista.stream().map(b -> new BackupSummaryDTO(b.getId(), b.getCreated(), b.getFrom(), b.getUntil(), b.getTables(), b.getFormat(), b.getSizeBytes(), b.getStatus(), b.getType())).toList());
    }

    /**
     * Lista las programaciones de respaldo activas. Solo ADMIN.
     *
     * @return lista de programaciones activas de respaldo
     */
    @GetMapping("/programacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BackupSchedule>> listSchedules() {
        return ResponseEntity.ok(progService.listActives());
    }
    /**
     * Crea una programación de respaldo y la agenda de inmediato si queda activa.
     * Solo ADMIN.
     *
     * @param req programación con frecuencia, tablas, formato y bandera de activa
     * @return programación creada con estado 201
     */
    @PostMapping("/programacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupSchedule> createSchedule(@RequestBody BackupSchedule req) {
        BackupSchedule created = progService.create(req);
        // Opcional: auto-programar al crear
        if (Boolean.TRUE.equals(created.getActive())) {
            progService.scheduleExecution(created.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Activa la ejecución programada de una programación de respaldo existente. Solo ADMIN.
     *
     * @param id id de la programación a agendar
     * @return mapa con id, bandera de activo y mensaje de confirmación
     */
    @PostMapping("/{id}/programar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> schedule(@PathVariable Long id) {
        progService.scheduleExecution(id);
        BackupSchedule schedule = progService.get(id);
        return ResponseEntity.ok(Map.of(
                "id", schedule.getId(),
                "activo", Boolean.TRUE.equals(schedule.getActive()),
                "mensaje", "Programación de respaldo activada exitosamente"));
    }
    /**
     * Elimina el registro de un respaldo generado, sin tocar las programaciones. Solo ADMIN.
     *
     * @param id id del respaldo a eliminar
     * @return respuesta vacía con estado 204 si se eliminó
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBackup(@PathVariable Long id) {
        // Solo elimina el registro de backup. La programación usa su propio endpoint
        // para evitar colisión de IDs entre ambas tablas.
        backupService.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Elimina una programación de respaldo por su id, sin tocar los respaldos generados. Solo ADMIN.
     *
     * @param id id de la programación a eliminar
     * @return respuesta vacía con estado 204 si se eliminó
     */
    @DeleteMapping("/programacion/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        progService.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Descarga el archivo ZIP de un respaldo generado. Solo ADMIN.
     *
     * @param id id del respaldo a descargar
     * @return bytes del ZIP con cabecera de descarga backup-id.zip
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] content = backupService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup-" + id + ".zip")
                .contentLength(content.length)
                .body(content);
    }
    /**
     * Ejecuta de inmediato la programación indicada y guarda el respaldo automático resultante.
     * Solo ADMIN. Actualiza además la fecha de última ejecución de la programación.
     *
     * @param id id de la programación a ejecutar ahora
     * @return mapa con id del respaldo, id de programación y mensaje de confirmación
     */
    @PostMapping("/{id}/ejecutar-ahora")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> executeNow(@PathVariable Long id) {
        BackupSchedule p = progService.get(id);
        // Ejecutar backup inmediato con el rango correspondiente
          OffsetDateTime ahora = OffsetDateTime.now();
          OffsetDateTime from;
          OffsetDateTime until;
        if (p.getEveryTimes() != null) {
            from = ahora.minusHours(p.getEveryTimes());
            until = ahora;
        } else {
            from = ahora.minusDays(p.getEveryDays()).withHour(0).withMinute(0).withSecond(0).withNano(0);
            until = ahora.withHour(23).withMinute(59).withSecond(59);
        }
        Set<String> tables = (p.getTables() != null && !p.getTables().isEmpty()) ? p.getTables() : Set.of();
        Backup backup = backupService.generateBackup(from, until, tables, p.getFormat(), "automatico");
        // Actualizar última ejecución en la programación (usa método interno del service)
        progService.updateLastExecution(id, backup.getCreated());
        return ResponseEntity.ok(Map.of(
                "id", backup.getId(),
                "programacionId", p.getId(),
                "mensaje", "El respaldo se ejecutó y guardó correctamente"));
    }

      /**
       * Body de POST /api/v1/admin/backups (filtros del respaldo a generar).
       * Campos: desde, hasta, tablas, formato y tipo ("manual" o "automatico").
       */
      @Data @NoArgsConstructor @AllArgsConstructor
      public static class BackupRequestDTO {
        @NotNull @JsonProperty("desde") @JsonDeserialize(using = FlexibleOffsetDateTimeDeserializer.class) OffsetDateTime from;
        @NotNull @JsonProperty("hasta") @JsonDeserialize(using = FlexibleOffsetDateTimeDeserializer.class) OffsetDateTime until;
        @NotEmpty @JsonProperty("tablas") Set<String> tables;
        @NotNull @JsonProperty("formato") String format;
        @JsonProperty("tipo") String type; // "manual" o "automatico", default "manual"
    }
      /**
       * Fila del listado de respaldos generados.
       * Campos: id, created, from, until, tables, format, path, sizeBytes,
       * status, type y urlDescarga.
       */
      @Data @NoArgsConstructor @AllArgsConstructor
      public static class BackupResponseDTO {
          Long id;
          @JsonProperty("creadoEn")
          OffsetDateTime created;
          @JsonProperty("desde")
          OffsetDateTime from;
          @JsonProperty("hasta")
          OffsetDateTime until;
          @JsonProperty("tablas")
          Set<String> tables;
          @JsonProperty("formato")
          String format;
          @JsonProperty("ruta")
          String path;
          @JsonProperty("tamanoBytes")
          Long sizeBytes;
          @JsonProperty("estado")
          String status;
          @JsonProperty("tipo")
          String type;
          String urlDescarga;
      }
      /**
       * Resumen de un respaldo para el listado (sin ruta ni URL de descarga).
       * Campos: id, created, from, until, tables, format, sizeBytes,
       * status y type.
       */
      @Data @AllArgsConstructor
      public static class BackupSummaryDTO {
          /**
           * Constructor sin argumentos para Jackson.
           */
          public BackupSummaryDTO() {
          }
          Long id;
          @JsonProperty("creadoEn")
          OffsetDateTime created;
          @JsonProperty("desde")
          OffsetDateTime from;
          @JsonProperty("hasta")
          OffsetDateTime until;
          @JsonProperty("tablas")
          Set<String> tables;
          @JsonProperty("formato")
          String format;
          @JsonProperty("tamanoBytes")
          Long sizeBytes;
          @JsonProperty("estado")
          String status;
          @JsonProperty("tipo")
          String type;
      }

    private static OffsetDateTime parseFlexible(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim().replace(' ', 'T');
          try { return OffsetDateTime.parse(text); } catch (Exception ignored) {
              // best-effort: se intenta el siguiente formato flexible
          }
        try {
            java.time.format.DateTimeFormatter fmt = new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').appendPattern("HH:mm")
                    .optionalStart().appendPattern(":ss").optionalEnd()
                    .parseDefaulting(java.time.temporal.ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(java.time.temporal.ChronoField.OFFSET_SECONDS, java.time.ZoneOffset.of("-05:00").getTotalSeconds()).toFormatter();
            return OffsetDateTime.parse(text, fmt);
        } catch (Exception e) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Formato de fecha inválido: " + text); }
    }
}
