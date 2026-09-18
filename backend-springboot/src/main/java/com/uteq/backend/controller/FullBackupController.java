package com.uteq.backend.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uteq.backend.entity.ConfigurationBackup;
import com.uteq.backend.entity.RegistrationBackup;
import com.uteq.backend.service.FullBackupService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/respaldo-completo")
public class FullBackupController {

    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_DETALLE = "detalle";

    private final FullBackupService service;

    /**
     * Constructor con el servicio de respaldo completo.
     *
     * @param service servicio de configuración y registros del respaldo completo
     */
    public FullBackupController(FullBackupService service) {
        this.service = service;
    }

    // ── Configuración DR ──────────────────────────────────────────────────────
    /**
     * Devuelve la configuración de recuperación ante desastres del respaldo completo. Solo ADMIN.
     *
     * @return configuración vigente con frecuencia, retención y habilitado
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationBackup> getConfig() {
        return ResponseEntity.ok(service.getConfiguration());
    }
    /**
     * Actualiza la frecuencia, retención y habilitado del respaldo completo. Solo ADMIN.
     *
     * @param req frecuencia en horas, días de retención y bandera de habilitado
     * @return configuración actualizada del respaldo completo
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationBackup> updateConfig(@RequestBody ConfigRequestDTO req) {
        return ResponseEntity.ok(service.updateConfiguration(req.frequencyTimes, req.daysRetention, req.enabled));
    }

    // ── Historial de registros ─────────────────────────────────────────────────
    /**
     * Lista el historial de ejecuciones del respaldo completo, opcionalmente filtrado por tipo.
     * Solo ADMIN.
     *
     * @param type tipo de respaldo a filtrar, null para todos
     * @return lista de registros del historial solicitado
     */
    @GetMapping("/registros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationBackup>> listRegistrations(
            @RequestParam(name = "tipo", required = false) String type) {
        List<RegistrationBackup> lista = (type != null && !type.isBlank())
                ? service.listByType(type)
                : service.listAll();
        return ResponseEntity.ok(lista);
    }
    /**
     * Elimina un registro del historial del respaldo completo. Solo ADMIN.
     *
     * @param id id del registro a eliminar
     * @return respuesta vacía con estado 204 si se eliminó
     */
    @DeleteMapping("/registros/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Descarga el archivo ZIP de una ejecución del respaldo completo. Solo ADMIN.
     *
     * @param id id del registro cuyo archivo se solicita
     * @return bytes del ZIP con cabecera de descarga
     */
    @GetMapping("/registros/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRegistration(@PathVariable Long id) {
        byte[] content = service.download(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup-completo-" + id + ".zip")
                .contentLength(content.length)
                .body(content);
    }

    // ── Registro de ejecución (llamado desde el microservicio Node.js vía token interno) ──
    /**
     * Registra el inicio de una ejecución del respaldo completo desde el microservicio Node.
     * Solo ADMIN.
     *
     * @param req tipo de respaldo y usuario que lo ejecuta
     * @return registro de inicio creado
     */
    @PostMapping("/registros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegistrationBackup> registerStart(@RequestBody RegistrationStartDTO req) {
        return ResponseEntity.ok(service.registerStart(req.type, req.executedBy));
    }
    /**
     * Registra el resultado final de una ejecución del respaldo completo. Solo ADMIN.
     *
     * @param id id del registro de ejecución a cerrar
     * @param req estado final, nombre y tamaño del archivo, ruta y mensaje de error
     * @return registro actualizado con el resultado de la ejecución
     */
    @PutMapping("/registros/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegistrationBackup> registerResult(
            @PathVariable Long id, @RequestBody RegistrationResultDTO req) {
        return ResponseEntity.ok(service.registerResult(
                id, req.status, req.nameFile, req.sizeFileBytes, req.pathR2, req.messageError));
    }

    // ── Proxy hacia el microservicio Node.js ───────────────────────────────────
    /**
     * Dispara un respaldo completo en el microservicio Node y actúa como proxy de su respuesta.
     * Solo ADMIN. Devuelve 503 en JSON si el microservicio no responde.
     *
     * @param principal identidad autenticada que solicita el respaldo
     * @return mapa con mensaje y detalle de la respuesta del microservicio
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> triggerBackupFull(java.security.Principal principal) {
        String backupServiceUrl = System.getenv("BACKUP_SERVICE_URL");
        if (backupServiceUrl == null || backupServiceUrl.isBlank()) {
            backupServiceUrl = "http://localhost:3000";
        }

        try {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(90000); // 90 segundos para tolerar el cold-start de Render
            org.springframework.web.client.RestTemplate restTemplate =
                    new org.springframework.web.client.RestTemplate(factory);
            java.util.Map<String, Object> reqBody = new java.util.HashMap<>();
            if (principal != null) {
                // The frontend doesn't send the user ID in the proxy request, so we need to
                // pass a dummy ID or find it if we injected the user repo. But since we are proxying,
                // and the Node service can handle null usuarioId if not found, we can just send null
                // or try to fetch it if we had the repo. To keep it simple and compile-safe:
                reqBody.put("usuarioId", null);
            }
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            String internalApiKey = System.getenv("INTERNAL_API_KEY");
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("x-internal-api-key", internalApiKey);
            }
            
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(reqBody, headers);
            
            org.springframework.http.ResponseEntity<String> nodeResponse = restTemplate.postForEntity(
                backupServiceUrl + "/api/v1/trigger",
                requestEntity,
                String.class
            );
            if (nodeResponse.getStatusCode().value() == 429) {
                return ResponseEntity.status(429).body(java.util.Map.of(CLAVE_MENSAJE, "Ya hay un respaldo en ejecucion", CLAVE_DETALLE, nodeResponse.getBody() == null ? "" : nodeResponse.getBody()));
            }
            return ResponseEntity.status(nodeResponse.getStatusCode())
                    .body(java.util.Map.of(CLAVE_MENSAJE, "Backup completo iniciado",
                            CLAVE_DETALLE, nodeResponse.getBody() == null ? "" : nodeResponse.getBody()));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            int sc = e.getStatusCode().value();
            if (sc == 429) return ResponseEntity.status(429).body(java.util.Map.of(CLAVE_MENSAJE, "Ya hay un respaldo en ejecucion", CLAVE_DETALLE, e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of(CLAVE_MENSAJE, "Microservicio de respaldos no disponible", CLAVE_DETALLE, e.getResponseBodyAsString()));
        } catch (Exception e) {
            // Devolver JSON 503 en vez de texto HTML para que el frontend no rompa el parse.
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of(CLAVE_MENSAJE, "Microservicio de respaldos no disponible",
                            CLAVE_DETALLE, e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    // ── DTOs ───────────────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ConfigRequestDTO {
        @JsonProperty("frecuenciaHoras")
        Integer frequencyTimes;
        @JsonProperty("diasRetencion")
        Integer daysRetention;
        @JsonProperty("habilitado")
        Boolean enabled;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegistrationStartDTO {
        @JsonProperty("tipo")
        String type;
        @JsonProperty("ejecutadoPor")
        Long executedBy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegistrationResultDTO {
        @JsonProperty("estado")
        String status;
        @JsonProperty("nombreArchivo")
        String nameFile;
        @JsonProperty("tamanoArchivoBytes")
        Long sizeFileBytes;
        @JsonProperty("rutaR2")
        String pathR2;
        @JsonProperty("mensajeError")
        String messageError;
    }
}
