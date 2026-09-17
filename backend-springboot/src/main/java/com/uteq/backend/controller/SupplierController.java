package com.uteq.backend.controller;

import com.uteq.backend.dto.SupplierRequestDTO;
import com.uteq.backend.dto.SupplierResponseDTO;
import com.uteq.backend.entity.Supplier;
import com.uteq.backend.repository.SupplierRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores")
public class SupplierController {

    private final SupplierRepository supplierRepository;

    /**
     * Constructor con el repositorio de proveedores.
     *
     * @param supplierRepository repositorio del catálogo de proveedores
     */
    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }
    /**
     * Lista en forma paginada los proveedores con filtros por nombre y estado activo.
     * Solo GERENTE y ADMIN. Sin filtros usa el listado paginado directo.
     *
     * @param q texto a buscar en el nombre, null para no filtrar
     * @param active true solo activos, false solo inactivos, null todos
     * @param pageable paginación y orden solicitados
     * @return página de proveedores que cumplen los filtros
     */

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<Page<SupplierResponseDTO>> list(
            @RequestParam(required = false) String q,
            @RequestParam(name = "activo", required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        String filter = (q != null && !q.isBlank()) ? q.trim() : null;
        Boolean activeFilter = active;
        // Si no hay filtros, usar findAll paginado (mas eficiente)
        if (filter == null && activeFilter == null) {
            return ResponseEntity.ok(supplierRepository.findAll(pageable).map(this::toDTO));
        }
        Page<SupplierResponseDTO> page = supplierRepository.searchWithFilters(filter, activeFilter, pageable).map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    // Compatibilidad: lista completa para casos antiguos (no usar con 50k)
    /**
     * Lista todos los proveedores sin paginar para compatibilidad con clientes antiguos.
     * Solo GERENTE y ADMIN.
     *
     * @return lista completa de proveedores
     */
    @GetMapping("/todo")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<SupplierResponseDTO>> listAll() {
        List<SupplierResponseDTO> suppliers = supplierRepository.findAll().stream()
                .map(this::toDTO).toList();
        return ResponseEntity.ok(suppliers);
    }
    /**
     * Busca hasta cinco proveedores cuyo nombre contenga el texto dado, sin distinguir mayúsculas.
     * Solo GERENTE y ADMIN.
     *
     * @param q texto parcial del nombre del proveedor
     * @return lista de hasta cinco proveedores coincidentes
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<List<SupplierResponseDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(
                supplierRepository.findTop5ByNameContainingIgnoreCase(q).stream()
                        .map(this::toDTO)
                        .toList());
    }
    /**
     * Crea un proveedor nuevo si el nombre y el RUC no existen. Solo GERENTE y ADMIN.
     *
     * @param dto nombre, RUC, dirección y contacto del proveedor
     * @return proveedor creado con su id y cabecera de ubicación
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<SupplierResponseDTO> create(@Valid @RequestBody SupplierRequestDTO dto) {
        if (supplierRepository.existsByNameIgnoreCase(dto.name())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (dto.ruc() != null && !dto.ruc().isBlank() && supplierRepository.existsByRucIgnoreCase(dto.ruc())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        Supplier p = new Supplier();
        p.setName(dto.name());
        p.setRuc(dto.ruc());
        p.setDireccion(dto.direccion());
        p.setTelefono(dto.telefono());
        p.setEmail(dto.email());
        p.setPersonaContacto(dto.personaContacto());
        p.setActive(dto.active() != null ? dto.active() : true);
        Supplier guardado = supplierRepository.save(p);
        return ResponseEntity.created(URI.create("/api/v1/proveedores/" + guardado.getId()))
                .body(toDTO(guardado));
    }
    /**
     * Actualiza los datos de un proveedor existente. Solo GERENTE y ADMIN.
     *
     * @param id id del proveedor a actualizar
     * @param dto nombre, RUC, dirección y contacto nuevos del proveedor
     * @return proveedor actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ResponseEntity<SupplierResponseDTO> update(@PathVariable Integer id,
                                                           @Valid @RequestBody SupplierRequestDTO dto) {
        return supplierRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.name());
                    existing.setRuc(dto.ruc());
                    existing.setDireccion(dto.direccion());
                    existing.setTelefono(dto.telefono());
                    existing.setEmail(dto.email());
                    existing.setPersonaContacto(dto.personaContacto());
                    if (dto.active() != null) existing.setActive(dto.active());
                    Supplier guardado = supplierRepository.save(existing);
                    return ResponseEntity.ok(toDTO(guardado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private SupplierResponseDTO toDTO(Supplier p) {
        return new SupplierResponseDTO(
                p.getId(), p.getName(), p.getRuc(), p.getDireccion(),
                p.getTelefono(), p.getEmail(), p.getPersonaContacto(), p.getActive());
    }
}
