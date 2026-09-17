package com.uteq.backend.service;

import com.uteq.backend.entity.Backup;
import com.uteq.backend.repository.BackupRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final int MAX_DIAS = 30;
    private static final String TABLA_RESERVAS = "reservas";
    private static final String TABLA_RESERVACIONES = "reservaciones";
    private static final String TABLA_BITACORA = "bitacora_auditoria";
    private static final String TABLA_AUDITORIA = "auditoria";
    private static final Set<String> TABLAS_PERMITIDAS = Set.of(
            "prestamos", TABLA_RESERVAS, TABLA_RESERVACIONES, "multas", "libros", "usuarios",
            TABLA_BITACORA, TABLA_AUDITORIA, "configuracion_sistema",
            "notificaciones", "favoritos", "sugerencias_adquisicion", "categorias", "autores"
    );

    private static final Map<String, String> TABLA_COL = Map.ofEntries(
            Map.entry("prestamos", "fecha_prestamo"),
            Map.entry(TABLA_RESERVAS, "fecha_reserva"),
            Map.entry(TABLA_RESERVACIONES, "fecha_reserva"),
            Map.entry("multas", "fecha_generada"),
            Map.entry(TABLA_BITACORA, "fecha_hora"),
            Map.entry(TABLA_AUDITORIA, "fecha_hora"),
            Map.entry("libros", "fecha_registro"),
            Map.entry("usuarios", "fecha_registro"),
            Map.entry("notificaciones", "creado_en"),
            Map.entry("favoritos", "agregado_en"),
            Map.entry("sugerencias_adquisicion", "creado_en")
            // categorias, autores, configuracion_sistema -> sin fecha, volcado completo
    );

    private static final int MAX_DETALLE_CHARS = 500;

    private final BackupRepository backupRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BackupStorageService storageService;

    @Value("${app.backup.r2.bucket:}")
    private String bucket;

    /**
     * Constructor con repositorios, JDBC y almacenamiento.
     *
     * @param backupRepository repositorio de respaldos
     * @param userRepository repositorio de usuarios
     * @param jdbcTemplate plantilla JDBC para volcados
     * @param storageService servicio de almacenamiento R2
     */
    public BackupService(BackupRepository backupRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate, BackupStorageService storageService) {
        this.backupRepository = backupRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime until) {
        if (from == null || until == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desde y hasta son obligatorios");
        if (from.isAfter(until)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desde debe ser anterior a hasta");
        long days = ChronoUnit.DAYS.between(from, until);
        if (days > MAX_DIAS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango max 30 dias, solicitados: " + days);
        // No se valida "hasta no puede ser futuro" para evitar problemas de zona horaria:
        // un usuario en -05:00 que selecciona "31 Aug 22:00" envía 2026-09-01T03:00Z,
        // que el servidor en UTC ve como futuro aunque localmente no lo es.
        // Si "hasta" está en el futuro, la consulta SQL simplemente devuelve todos los
        // registros hasta "ahora", que es el comportamiento correcto.
    }

    private void validateTables(Set<String> tables) {        if (tables == null || tables.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar al menos una tabla");
        for (String t : tables) if (!TABLAS_PERMITIDAS.contains(t)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tabla no permitida: " + t);
    }

    /**
     * Genera un respaldo comprimido de las tablas pedidas en el rango dado y lo registra como completado.
     * Valida que el rango no supere 30 días y que las tablas estén permitidas, vuelca cada tabla por JDBC
     * en SQL o CSV dentro de un zip, lo sube al almacenamiento (cifrado si está habilitado) y persiste
     * la fila con el usuario autenticado como creador.
     *
     * @param from inicio del rango temporal del volcado, inclusivo y anterior a {@code until}
     * @param until fin del rango temporal del volcado, inclusivo
     * @param tables tablas lógicas a respaldar; cada una debe figurar entre las permitidas
     * @param format formato del volcado, {@code sql} o {@code csv}; nulo equivale a {@code sql}
     * @param type origen del respaldo, por ejemplo manual o automático, guardado tal cual en la fila
     * @return el respaldo persistido con ruta de almacenamiento, tamaño y estado COMPLETADO
     * @throws org.springframework.web.server.ResponseStatusException con 400 si el rango, las tablas o el formato son inválidos
     */
    @Transactional
    public Backup generateBackup(OffsetDateTime from, OffsetDateTime until, Set<String> tables, String format, String type) {
        validateRange(from, until);
        validateTables(tables);
        String fmt = format == null ? "sql" : format.toLowerCase();
        if (!fmt.equals("sql") && !fmt.equals("csv")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formato debe ser sql o csv");

        byte[] zipBytes = generateZip(from, until, tables, fmt);
        String key = "backups/backup_" + System.currentTimeMillis() + ".zip";
        if (storageService.isEncryptionEnabled()) key += ".enc";
        storageService.upload(key, zipBytes);

        Backup backup = Backup.builder()
                .createdBy(getUserCurrentId())
                .from(from)
                .until(until)
                .tables(new HashSet<>(tables))
                .format(fmt)
                .path(key)
                .sizeBytes((long) zipBytes.length)
                .status("COMPLETADO")
                .type(type)
                .created(OffsetDateTime.now())
                .build();
        return backupRepository.save(backup);
    }

    private byte[] generateZip(OffsetDateTime from, OffsetDateTime until, Set<String> tables, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (String table : tables) {
                String col = TABLA_COL.get(table);
                String phys = physTable(table);
                List<Map<String, Object>> rows;
                try {
                    if (col != null) {
                        rows = jdbcTemplate.queryForList("SELECT * FROM " + phys + " WHERE " + col + " >= ? AND " + col + " <= ?", from, until);
                    } else {
                        // sin columna de fecha (categorias, autores, configuracion_sistema) -> volcado completo
                        rows = jdbcTemplate.queryForList("SELECT * FROM " + phys);
                    }
                } catch (org.springframework.dao.DataAccessException e) {
                    // Convertir el 500 por columna inexistente en 400 legible (B10: categorias, multas, autores).
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No se pudo filtrar la tabla " + table + " por columna " + col + ": " + e.getMostSpecificCause().getMessage());
                }
                String ext = fmtExt(format);
                zos.putNextEntry(new ZipEntry(table + "." + ext));
                String content = format.equals("sql") ? toSql(physTable(table), rows) : toCsv(rows);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generando zip", e);
        }
    }

    private String physName(String logical) {
        if (logical.equals("reservas")) return "reservaciones";
        if (logical.equals("auditoria")) return "bitacora_auditoria";
        return logical;
    }
    private String physTable(String logical) {
        if (logical.equals("reservas") || logical.equals("reservaciones")) return "reservaciones";
        if (logical.equals("auditoria") || logical.equals("bitacora_auditoria")) return "bitacora_auditoria";
        return logical;
    }
    private String fmtExt(String f) { return f.equals("sql") ? "sql" : "csv"; }

    private String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", rows.get(0).keySet())).append("\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.values().stream().map(v -> {
                if (v == null) return "";
                String s = truncateText(v.toString());
                s = s.replace("\"", "\"\"");
                if (s.contains(",") || s.contains("\n") || s.contains("\"")) return "\"" + s + "\"";
                return s;
            }).collect(Collectors.joining(","))).append("\n");
        }
        return sb.toString();
    }

    private String toSql(String table, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return "-- sin filas " + table + "\n";
        StringBuilder sb = new StringBuilder("-- tabla " + table + "\n");
        for (Map<String, Object> r : rows) {
            String cols = String.join(", ", r.keySet());
            String vals = r.values().stream().map(v -> {
                if (v == null) return "NULL";
                if (v instanceof Number) return v.toString();
                if (v instanceof Boolean) return (Boolean) v ? "TRUE" : "FALSE";
                return "'" + truncateText(v.toString()).replace("'", "''") + "'";
            }).collect(Collectors.joining(", "));
            sb.append("INSERT INTO ").append(table).append(" (").append(cols).append(") VALUES (").append(vals).append(");\n");
        }
        return sb.toString();
    }

    /**
         * Lista todos los backups registrados.
     * @return lista de backups ordenados por fecha descendente
     */

    public List<Backup> listAll() { return backupRepository.findAllOrderByCreatedDesc(); }
    /**
     * Lista los respaldos cuyo rango cae dentro del intervalo dado, para ubicar copias por fecha.
     *
     * @param from inicio del intervalo de búsqueda de respaldos
     * @param until fin del intervalo de búsqueda de respaldos
     * @return respaldos generados dentro del intervalo solicitado
     */
    public List<Backup> listByRange(OffsetDateTime from, OffsetDateTime until) { return backupRepository.findByDateRange(from, until); }
    /**
         * Obtiene un backup por ID.
     * @param id identificador
     * @return backup encontrado
     * @throws ResponseStatusException si no existe (404)
     */
    public Backup getById(Long id) { return backupRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup no encontrado " + id)); }

    /**
     * Trunca textos largos para evitar que la bitacora de auditoria genere zips gigantes.
     * El campo detalles puede traer dumps previos de 10k caracteres que luego se encriptan
     * y rompen la visualización del frontend.
     */
    private String truncateText(String text) {
        if (text != null && text.length() > MAX_DETALLE_CHARS) {
            return text.substring(0, MAX_DETALLE_CHARS) + "...(truncado)";
        }
        return text;
    }
    /**
     * Elimina un respaldo junto con su archivo en el almacenamiento.
     * El borrado del archivo es best-effort: la fila se elimina aunque el almacenamiento falle.
     *
     * @param id identificador del respaldo a eliminar
     * @throws org.springframework.web.server.ResponseStatusException con 404 si el respaldo no existe
     */
    @Transactional
    public void delete(Long id) {        Backup b = getById(id);
          try { storageService.delete(b.getPath()); } catch (Exception ignored) {
              // best-effort: el registro se elimina aunque falle el storage
          }
        backupRepository.delete(b);
    }

    /**
     * Descarga los bytes del zip de un respaldo desde el almacenamiento, descifrándolo si el cifrado
     * está habilitado.
     *
     * @param id identificador del respaldo cuyo archivo se quiere descargar
     * @return bytes del archivo de respaldo listos para enviar al cliente
     * @throws org.springframework.web.server.ResponseStatusException con 404 si el respaldo no existe
     */

    public byte[] download(Long id) {
        Backup b = getById(id);
        return storageService.download(b.getPath());
    }

    private Long getUserCurrentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return 1L;
        Object p = auth.getPrincipal();
        try {
            var f = p.getClass().getDeclaredField("id");
            f.setAccessible(true);
            return (Long) f.get(p);
        } catch (Exception e) {
            try { return userRepository.findByEmail(auth.getName()).map(u -> u.getId()).orElse(1L); } catch (Exception ex) { return 1L; }
        }
    }
}
