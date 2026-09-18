package com.uteq.backend.service;

import com.uteq.backend.entity.ConfigurationBackup;
import com.uteq.backend.entity.RegistrationBackup;
import com.uteq.backend.repository.ConfigurationBackupRepository;
import com.uteq.backend.repository.RegistrationBackupRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class FullBackupService {

    private final ConfigurationBackupRepository configRepo;
    private final RegistrationBackupRepository registrationRepo;
    private final UserRepository userRepository;

    private final BackupStorageService storageService;

    /**
     * Constructor con los repositorios de configuración y registros de respaldo más el almacenamiento.
     *
     * @param configRepo repositorio de la configuración del respaldo completo
     * @param registrationRepo repositorio del historial de ejecuciones de respaldo
     * @param userRepository repositorio de usuarios para registrar quién actualiza la configuración
     * @param storageService servicio de almacenamiento remoto o local de los archivos
     */
    public FullBackupService(ConfigurationBackupRepository configRepo,
                                   RegistrationBackupRepository registrationRepo,
                                   UserRepository userRepository,
                                   BackupStorageService storageService) {
        this.configRepo = configRepo;
        this.registrationRepo = registrationRepo;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    // ── Configuración ────────────────────────────────────────────────────────
    /**
     * Recupera la configuración del respaldo completo, creándola con valores apagados por defecto
     * (deshabilitada, cada 6 horas, 14 días de retención) cuando aún no existe ninguna fila.
     *
     * @return la configuración vigente del respaldo completo
     */
    public ConfigurationBackup getConfiguration() {
        return configRepo.findAll().stream().findFirst().orElseGet(() -> {
            ConfigurationBackup config = ConfigurationBackup.builder()
                    .enabled(false)
                    .frequencyTimes(6)
                    .daysRetention(14)
                    .build();
            return configRepo.save(config);
        });
    }
    /**
     * Actualiza la frecuencia, la retención y el encendido del respaldo completo registrando al
     * usuario autenticado y la fecha del cambio. Si queda habilitada, recalcula la próxima ejecución
     * desde ahora más la frecuencia. Los parámetros nulos conservan su valor actual.
     *
     * @param frequencyTimes frecuencia en horas entre ejecuciones, entre 1 y 168; nulo la conserva
     * @param daysRetention días de retención de los archivos, entre 1 y 90; nulo la conserva
     * @param enabled verdadero para activar el respaldo programado, falso para pausarlo; nulo lo conserva
     * @return la configuración con los valores actualizados
     * @throws org.springframework.web.server.ResponseStatusException con 400 si algún valor está fuera de rango
     */
    @Transactional
    public ConfigurationBackup updateConfiguration(Integer frequencyTimes, Integer daysRetention, Boolean enabled) {
        if (frequencyTimes != null && (frequencyTimes < 1 || frequencyTimes > 168)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frecuenciaHoras debe estar entre 1 y 168");
        }
        if (daysRetention != null && (daysRetention < 1 || daysRetention > 90)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diasRetencion debe estar entre 1 y 90");
        }
        ConfigurationBackup config = getConfiguration();
        if (frequencyTimes != null) config.setFrequencyTimes(frequencyTimes);
        if (daysRetention != null) config.setDaysRetention(daysRetention);
        if (enabled != null) config.setEnabled(enabled);
        config.setUpdatedBy(getUserCurrentId());
        config.setUpdated(OffsetDateTime.now());
        if (Boolean.TRUE.equals(config.getEnabled())) {
            config.setNextExecution(OffsetDateTime.now().plusHours(config.getFrequencyTimes()));
        }
        return configRepo.save(config);
    }

    // ── Historial de registros ────────────────────────────────────────────────
    /**
         * Lista los registros de backup filtrados por tipo.
     *
     * @param type tipo de backup a filtrar (ej. 'completo', 'incremental')
     * @return lista de registros ordenados por inicio descendente
     */
    public List<RegistrationBackup> listByType(String type) {
        return registrationRepo.findByTypeOrderByStartedDesc(type);
    }

    /**
         * Lista todos los backups registrados.
     * @return lista de backups ordenados por fecha descendente
     */

    public List<RegistrationBackup> listAll() {
        return registrationRepo.findAll();
    }
    /**
         * Elimina un registro de backup y su archivo en almacenamiento.
     *
     * @param id identificador del registro a eliminar
     * @throws ResponseStatusException si el registro no existe (404)
     */
    @Transactional
    public void delete(Long id) {
        RegistrationBackup r = registrationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));
        if (r.getPathR2() != null) {
            try {
                String key = extractKey(r.getPathR2());
                storageService.delete(key);
            } catch (Exception ignored) {
            }
        }
        registrationRepo.delete(r);
    }

    /**
     * Descarga los bytes del archivo asociado a un registro de respaldo completo, resolviendo su
     * clave desde la ruta guardada (incluidas rutas remotas con esquema {@code s3://}).
     *
     * @param id identificador del registro de respaldo cuyo archivo se quiere descargar
     * @return bytes del archivo de respaldo listos para enviar al cliente
     * @throws org.springframework.web.server.ResponseStatusException con 404 si el registro no existe y con 400 si no tiene archivo asociado
     */

    public byte[] download(Long id) {
        RegistrationBackup r = registrationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));
        if (r.getPathR2() == null || r.getPathR2().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay archivo asociado a este registro");
        }
        String key = extractKey(r.getPathR2());
        return storageService.download(key);
    }

    private String extractKey(String path) {
        // Node.js retorna "s3://bucket/backups/..." o una ruta local.
        if (path.startsWith("s3://")) {
            String withoutEsquema = path.substring(5);
            int idx = withoutEsquema.indexOf('/');
            if (idx != -1 && idx < withoutEsquema.length() - 1) {
                return withoutEsquema.substring(idx + 1);
            }
        }
        return path;
    }

    // ── Registro de ejecución (llamado desde el microservicio Node.js via token interno) ──
    /**
     * Registra el inicio de una ejecución del microservicio externo de respaldo como fila en estado
     * {@code ejecutando}, para seguir su resultado cuando el microservicio informe por token interno.
     *
     * @param type tipo de respaldo iniciado, por ejemplo completo o incremental
     * @param executedBy identificador del ejecutor informado por el microservicio
     * @return el registro de ejecución recién creado en estado ejecutando
     */
    @Transactional
    public RegistrationBackup registerStart(String type, Long executedBy) {
        RegistrationBackup r = RegistrationBackup.builder()
                .type(type)
                .status("ejecutando")
                .executedBy(executedBy)
                .build();
        return registrationRepo.save(r);
    }
    /**
     * Cierra una ejecución de respaldo con su estado final, archivo y error si lo hubo.
     * Cuando el resultado es exitoso avanza además la última y próxima ejecución de la configuración.
     *
     * @param id identificador del registro de ejecución iniciado por {@link #registerStart}
     * @param status estado final informado, por ejemplo exitoso o fallido
     * @param nameFile nombre del archivo de respaldo generado, si lo hubo
     * @param sizeBytes tamaño en bytes del archivo generado, si lo hubo
     * @param pathR2 ruta del archivo en el almacenamiento, si lo hubo
     * @param messageError mensaje del error ocurrido, si la ejecución falló
     * @return el registro de ejecución con el resultado y la fecha de cierre
     * @throws org.springframework.web.server.ResponseStatusException con 404 si el registro no existe
     */
    @Transactional
    public RegistrationBackup registerResult(Long id, String status, String nameFile,
                                               Long sizeBytes, String pathR2, String messageError) {
        RegistrationBackup r = registrationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado: " + id));
        r.setStatus(status);
        r.setNameFile(nameFile);
        r.setSizeFileBytes(sizeBytes);
        r.setPathR2(pathR2);
        r.setMessageError(messageError);
        r.setFinished(OffsetDateTime.now());
        // Si fue exitoso, actualizar la configuración con la última ejecución
        if ("exitoso".equals(status)) {
            configRepo.findAll().stream().findFirst().ifPresent(config -> {
                config.setLastExecution(r.getFinished());
                config.setNextExecution(r.getFinished().plusHours(config.getFrequencyTimes()));
                configRepo.save(config);
            });
        }
        return registrationRepo.save(r);
    }

    private Long getUserCurrentId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return userRepository.findByEmail(auth.getName()).map(u -> u.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
