package com.uteq.backend.service;

import com.uteq.backend.dto.ConfigurationSystemResponseDTO;
import com.uteq.backend.entity.AuditLogAudit;
import com.uteq.backend.entity.ConfigurationSystem;
import com.uteq.backend.repository.AuditLogAuditRepository;
import com.uteq.backend.repository.ConfigurationSystemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parámetros del sistema con cache en memoria (invalidado por clave al escribir).
 * Consumidores: usar {@code obtenerValor*} en vez del repositorio directo para no perder el cache.
 */
@Service
public class ConfigurationSystemService {

    private static final String CLAVE_NO_ENCONTRADA = "Clave de configuración no encontrada: ";
    private static final String TABLA_CONFIG = "configuracion_sistema";

    private final ConfigurationSystemRepository repo;
    private final AuditLogAuditRepository auditLogAuditRepo;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Constructor con el repositorio de parámetros y el de bitácora para auditar cambios.
     *
     * @param repo repositorio de {@code configuracion_sistema}
     * @param auditLogAuditRepo repositorio de {@code bitacora_auditoria} para registrar actualizaciones
     */
    public ConfigurationSystemService(ConfigurationSystemRepository repo,
                                        AuditLogAuditRepository auditLogAuditRepo) {
        this.repo = repo;
        this.auditLogAuditRepo = auditLogAuditRepo;
    }
    /**
     * Lista todos los parámetros del sistema como pares clave-valor para el panel de administración.
     *
     * @return parámetros registrados con su clave y valor actuales
     */
    @Transactional(readOnly = true)
    public List<ConfigurationSystemResponseDTO> list() {
        return repo.findAll().stream()
                .map(c -> new ConfigurationSystemResponseDTO(c.getKey(), c.getValue()))
                .toList();
    }

    /**
     * Actualiza el valor de un parámetro existente, invalida su entrada en la caché en memoria
     * y deja constancia del cambio en la bitácora de auditoría sobre {@code configuracion_sistema}.
     *
     * @param key clave del parámetro a actualizar, debe existir en el catálogo
     * @param freshValue valor nuevo que reemplaza al anterior
     * @return el parámetro con su clave y valor actualizados
     * @throws jakarta.persistence.EntityNotFoundException si no existe ningún parámetro con esa clave
     */
    @Transactional
    public ConfigurationSystemResponseDTO update(String key, String freshValue) {
        ConfigurationSystem config = repo.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(CLAVE_NO_ENCONTRADA + key));
        config.setValue(freshValue);
        repo.save(config);
        cache.remove(key);
        registerAudit(null, null, "Actualización de configuración: " + key + " = " + freshValue);
        return new ConfigurationSystemResponseDTO(config.getKey(), config.getValue());
    }

    private void registerAudit(Long executorId, Long registrationId, String detalles) {
        AuditLogAudit event = AuditLogAudit.builder()
                .userId(executorId)
                .typeOperacion("UPDATE")
                .tableAfectada(TABLA_CONFIG)
                .registrationId(registrationId)
                .detalles(detalles)
                .dateTime(OffsetDateTime.now())
                .build();
        auditLogAuditRepo.save(event);
    }
    /**
     * Recupera el valor en texto de un parámetro usando la caché en memoria antes de ir a la base.
     * La entrada se guarda en caché tras la primera lectura hasta que {@link #update} la invalide.
     *
     * @param key clave del parámetro a recuperar
     * @return valor en texto guardado para esa clave
     * @throws jakarta.persistence.EntityNotFoundException si no existe ningún parámetro con esa clave
     */
    @Transactional(readOnly = true)
    public String getValue(String key) {
        String cacheado = cache.get(key);
        if (cacheado != null) {
            return cacheado;
        }
        String value = repo.findById(key)
                .orElseThrow(() -> new EntityNotFoundException(CLAVE_NO_ENCONTRADA + key))
                .getValue();
        cache.put(key, value);
        return value;
    }

    /**
     * Recupera un parámetro como entero para los topes numéricos del negocio (préstamos, renovaciones,
     * tamaños de archivo). Delegada en {@link #getValue} con conversión estricta a entero.
     *
     * @param key clave del parámetro numérico a recuperar
     * @return valor del parámetro convertido a entero
     * @throws jakarta.persistence.EntityNotFoundException si no existe ningún parámetro con esa clave
     * @throws IllegalStateException si el valor guardado no es numérico entero
     */

    public Integer getIntegerValue(String key) {
        String value = getValue(key);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Valor no numérico para la clave '" + key + "': " + value);
        }
    }

    /**
     * Recupera un parámetro como decimal para los montos configurables del negocio.
     * Delegada en {@link #getValue} con conversión estricta a decimal.
     *
     * @param key clave del parámetro decimal a recuperar
     * @return valor del parámetro convertido a decimal
     * @throws jakarta.persistence.EntityNotFoundException si no existe ningún parámetro con esa clave
     * @throws IllegalStateException si el valor guardado no es un decimal válido
     */

    public BigDecimal getValueDecimal(String key) {
        String value = getValue(key);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Valor no decimal para la clave '" + key + "': " + value);
        }
    }
}
