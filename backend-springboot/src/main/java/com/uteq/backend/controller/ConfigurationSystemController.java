package com.uteq.backend.controller;

import com.uteq.backend.dto.ConfigurationSystemRequestDTO;
import com.uteq.backend.dto.ConfigurationSystemResponseDTO;
import com.uteq.backend.service.ConfigurationSystemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Solo ADMIN: separa quién administra parámetros del sistema (ADMIN)
// de quién opera el día a día (GERENTE).
@RestController
@RequestMapping("/api/v1/configuracion")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigurationSystemController {

    private final ConfigurationSystemService service;

    /**
     * Constructor con el servicio de parámetros del sistema.
     *
     * @param service servicio de lectura y actualización de la configuración
     */
    public ConfigurationSystemController(ConfigurationSystemService service) {
        this.service = service;
    }
    /**
     * Lista todos los parámetros de configuración del sistema. Solo ADMIN.
     *
     * @return lista de parámetros con clave y valor actual
     */
    @GetMapping
    public ResponseEntity<List<ConfigurationSystemResponseDTO>> list() {
        return ResponseEntity.ok(service.list());
    }
    /**
     * Actualiza el valor del parámetro de configuración indicado por su clave. Solo ADMIN.
     *
     * @param key clave del parámetro a modificar
     * @param dto valor nuevo del parámetro
     * @return parámetro actualizado con su clave y valor
     */
    @PutMapping("/{clave}")
    public ResponseEntity<ConfigurationSystemResponseDTO> update(
            @PathVariable("clave") String key,
            @Valid @RequestBody ConfigurationSystemRequestDTO dto) {
        return ResponseEntity.ok(service.update(key, dto.value()));
    }
}
