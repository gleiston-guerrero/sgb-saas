package com.uteq.backend.service;

import com.uteq.backend.dto.TypeDamageDTO;
import com.uteq.backend.entity.CategoryDamage;
import com.uteq.backend.entity.TypeDamage;
import com.uteq.backend.repository.CategoryDamageRepository;
import com.uteq.backend.repository.TypeDamageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeDamageService {

    private final TypeDamageRepository typeDamageRepo;
    private final CategoryDamageRepository categoryDamageRepo;

    private TypeDamageDTO toDTO(TypeDamage t) {
        return new TypeDamageDTO(t.getId(), t.getName(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getTypeCost(), t.getValue());
    }
    /**
     * Lista todos los tipos de daño con su categoría y costo, incluidos los desactivados, para la
     * gestión del catálogo que cobra los daños en devolución.
     *
     * @return tipos de daño registrados con nombre, categoría, tipo de costo y valor
     */
    @Transactional(readOnly = true)
    public List<TypeDamageDTO> listAll() {
        return typeDamageRepo.findAll().stream().map(this::toDTO).toList();
    }
    /**
     * Lista los tipos de daño activos con su categoría y costo para el formulario de revisión en ventanilla.
     *
     * @return tipos de daño vigentes con nombre, categoría, tipo de costo y valor
     */
    @Transactional(readOnly = true)
    public List<TypeDamageDTO> listActives() {
        return typeDamageRepo.findByActiveTrue().stream().map(this::toDTO).toList();
    }
    /**
     * Da de alta un tipo de daño activo con su categoría y costo para cobrarlo en devoluciones.
     * Rechaza nombres duplicados y valida que el costo sea FIJO o PORCENTAJE con valor no negativo
     * (el porcentaje además no supera 100).
     *
     * @param name nombre del tipo de daño, único en el catálogo
     * @param categoryId identificador de la categoría de daño a la que pertenece
     * @param typeCost forma de cobro, {@code FIJO} en moneda o {@code PORCENTAJE} del precio base
     * @param value monto fijo o porcentaje a cobrar por este daño
     * @return el tipo de daño persistido en estado activo
     * @throws IllegalArgumentException si el nombre ya existe o el costo o valor son inválidos
     * @throws jakarta.persistence.EntityNotFoundException si la categoría de daño no existe
     */
    @Transactional
    public TypeDamageDTO create(String name, Integer categoryId, String typeCost, BigDecimal value) {
        if (typeDamageRepo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Ya existe un tipo de daño con el nombre: " + name);
        }
        CategoryDamage cat = categoryDamageRepo.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría de daño no encontrada: " + categoryId));
        validate(typeCost, value);
        TypeDamage type = new TypeDamage();
        type.setName(name);
        type.setCategory(cat);
        type.setTypeCost(typeCost);
        type.setValue(value);
        type.setActive(true);
        TypeDamage guardado = typeDamageRepo.save(type);
        return toDTO(guardado);
    }
    /**
     * Reemplaza nombre, categoría y costo de un tipo de daño existente para corregir su cobro.
     * Rechaza nombres usados por otro tipo y aplica la misma validación de costo que el alta.
     *
     * @param id identificador del tipo de daño a actualizar
     * @param name nombre nuevo, único entre los demás tipos del catálogo
     * @param categoryId identificador de la categoría de daño a la que pasa a pertenecer
     * @param typeCost forma de cobro, {@code FIJO} en moneda o {@code PORCENTAJE} del precio base
     * @param value monto fijo o porcentaje nuevo a cobrar por este daño
     * @return el tipo de daño con los datos actualizados
     * @throws IllegalArgumentException si el nombre pertenece a otro tipo o el costo o valor son inválidos
     * @throws jakarta.persistence.EntityNotFoundException si el tipo o la categoría no existen
     */
    @Transactional
    public TypeDamageDTO update(Integer id, String name, Integer categoryId, String typeCost, BigDecimal value) {
        TypeDamage type = typeDamageRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de daño no encontrado: " + id));
        typeDamageRepo.findByName(name)
                .filter(t -> !t.getId().equals(id))
                .ifPresent(t -> { throw new IllegalArgumentException("Ya existe otro tipo de daño con el nombre: " + name); });
        CategoryDamage cat = categoryDamageRepo.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría de daño no encontrada: " + categoryId));
        validate(typeCost, value);
        type.setName(name);
        type.setCategory(cat);
        type.setTypeCost(typeCost);
        type.setValue(value);
        TypeDamage guardado = typeDamageRepo.save(type);
        return toDTO(guardado);
    }

    private void validate(String typeCost, BigDecimal value) {
        if (!"FIJO".equals(typeCost) && !"PORCENTAJE".equals(typeCost)) {
            throw new IllegalArgumentException("tipoCosto debe ser FIJO o PORCENTAJE");
        }
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("valor debe ser >=0");
        if ("PORCENTAJE".equals(typeCost) && value.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("porcentaje no puede superar 100");
    }
    /**
     * Desactiva un tipo de daño (baja lógica) para que deje de ofrecerse en la revisión en ventanilla.
     * La fila y su historial de cobros se conservan.
     *
     * @param id identificador del tipo de daño a desactivar
     * @throws jakarta.persistence.EntityNotFoundException si el tipo de daño no existe
     */
    @Transactional
    public void delete(Integer id) {
        TypeDamage type = typeDamageRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de daño no encontrado: " + id));
        type.setActive(false);
        typeDamageRepo.save(type);
    }
}
