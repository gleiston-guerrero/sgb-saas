package com.uteq.backend.repository;

import com.uteq.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    // Nota: usuarios no tiene columna "estado" (42703); el estado normalizado
    // es estado_id FK a estados_usuario (V2). Usar JOIN estados_usuario para
    // leer eu.nombre como estado (ver queries de morosidad).
    /** Busca un usuario por su correo exacto. */
    Optional<User> findByEmail(String email);

    // Autocompletado de usuarios por correo parcial (ventanilla de préstamos).
    // Retorna los 3 usuarios más coincidentes, ordenados por nombre.
    /** Busca hasta 3 usuarios cuyo correo contenga el texto, ordenados por nombre. */
    List<User> findTop3ByEmailContainingIgnoreCaseOrderByNameAsc(String email);

    // Usado por CredencialQrService.resolverPorToken() al leer un QR
    // escaneado en ventanilla.
    /** Busca un usuario por su token QR de credencial. */
    Optional<User> findByCredentialQrToken(UUID credentialQrToken);

    // Búsqueda por nombre o correo para el listado paginado de administración.
    /** Pagina los usuarios cuyo nombre o correo contenga el texto dado, sin importar mayúsculas. */
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    // F8-gerente (V38): listado con filtro opcional + creador opcional.
    // JPQL con LOWER+CONCAT evita el bug PostgreSQL+Hibernate con
    // CAST(:param AS TEXT) "syntax error at or near $1" (Position:33) en
    // nativeQuery cuando el param es NULL (ver 7e81c1e6 baseline).
    /**
     * Pagina los usuarios cuyo nombre o correo contenga el filtro dado y creados por el usuario indicado.
     *
     * @param filter texto en nombre o correo, nulo o vacío sin filtro
     * @param createdBy identificador del creador, nulo todos
     * @param pageable paginación solicitada
     * @return página de usuarios coincidentes
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT u FROM User u WHERE "
                    + "(:filter IS NULL OR :filter = '' "
                    + "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%')) "
                    + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :filter, '%'))) "
                    + "AND (:createdBy IS NULL OR u.createdBy = :createdBy)")
    Page<User> searchWithFilters(
            @org.springframework.data.repository.query.Param("filter") String filter,
            @org.springframework.data.repository.query.Param("createdBy") Long createdBy,
            Pageable pageable);

    // Fix 404 Admin usuarios (Bloquear/Eliminar): fetch con JOIN FETCH evita
    // LazyInitialization en findById sin tocar paginación de buscarConFiltros.
    /**
     * Busca al usuario por ID cargando su estado y roles con JOIN FETCH.
     *
     * @param id identificador del usuario
     * @return usuario con estado y roles, o vacío si no existe
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT u FROM User u LEFT JOIN FETCH u.status LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithStatusAndRoles(
            @org.springframework.data.repository.query.Param("id") Long id);

    // Case-insensitive para resolver ejecutor desde JWT (evita 404 fantasma si
    // correo viene con mayúsculas/espacios).
    /** Busca un usuario por su correo sin importar mayúsculas. */
    Optional<User> findByEmailIgnoreCase(String email);

    // Auto-eliminación de cuentas no verificados: borra usuarios cuyo
    // correo no fue verificado dentro de las últimas 24 horas. Invocado
    // periódicamente por UsuarioScheduler. Migrada de nativeQuery a JPQL
    // (P4): DELETE masivo sin sintaxis especifica de motor.
    /**
     * Elimina los usuarios no verificados registrados antes del corte dado.
     *
     * @param cutoff fecha límite de registro
     * @return cantidad de cuentas eliminadas
     */
    @org.springframework.data.jpa.repository.Query(
            "DELETE FROM User u WHERE u.emailVerified = false AND u.dateRegistration < :cutoff")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteNotVerifiedsBefore(@org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff);


}
