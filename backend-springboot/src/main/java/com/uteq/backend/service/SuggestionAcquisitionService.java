package com.uteq.backend.service;

import com.uteq.backend.dto.SuggestionAcquisitionRequestDTO;
import com.uteq.backend.dto.SuggestionAcquisitionResponseDTO;
import com.uteq.backend.dto.SuggestionGroupedDTO;
import com.uteq.backend.entity.SuggestionAcquisition;
import com.uteq.backend.repository.SuggestionAcquisitionRepository;
import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// crear() resuelve el usuarioId desde el Authentication; cambiarEstado() registra quién revisó.
// La auditoria de esta tabla ya no se hace aqui: trg_auditoria_sugerencias_adquisicion
// (V49__auditoria_triggers_negocio.sql) audita INSERT/UPDATE/DELETE a nivel de motor.
@Service
public class SuggestionAcquisitionService {

    private static final String SUGERENCIA_NO_ENCONTRADA = "Sugerencia de adquisición no encontrada con id: ";
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado con correo: ";

    private final SuggestionAcquisitionRepository suggestionRepo;
    private final UserRepository userRepo;

    // Sorts legacy en español de clientes viejos o peticiones manuales
    // (ver 500 "No property 'creadoEn'"). Se traducen al atributo JPA;
    // el resto pasa igual. No se renombra nada.
    private static final Map<String, String> SORT_LEGACY = Map.of(
            "creadoEn", "created");

    private static Pageable sortTolerante(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return pageable;
        }
        Sort traducido = Sort.by(pageable.getSort().stream()
                .map(orden -> SORT_LEGACY.containsKey(orden.getProperty())
                        ? orden.withProperty(SORT_LEGACY.get(orden.getProperty()))
                        : orden)
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), traducido);
    }

    /**
     * Constructor con el repositorio de sugerencias y el de usuarios para resolver al solicitante.
     *
     * @param suggestionRepo repositorio de sugerencias de adquisición
     * @param userRepo repositorio de usuarios para resolver el dueño desde su correo
     */
    public SuggestionAcquisitionService(SuggestionAcquisitionRepository suggestionRepo,
                                         UserRepository userRepo) {
        this.suggestionRepo = suggestionRepo;
        this.userRepo = userRepo;
    }
    /**
     * Registra una sugerencia de compra en estado PENDIENTE a nombre del lector autenticado.
     * El ISBN vacío se guarda como nulo para no chocar con el formato exigido al darlo de alta.
     *
     * @param dto título, autor, ISBN opcional y justificación de la compra sugerida
     * @param authentication identidad autenticada del lector solicitante, dueña de la sugerencia
     * @return la sugerencia persistida en estado PENDIENTE sin revisor asignado
     * @throws jakarta.persistence.EntityNotFoundException si el usuario autenticado ya no existe
     */
    @Transactional
    public SuggestionAcquisitionResponseDTO create(SuggestionAcquisitionRequestDTO dto, Authentication authentication) {
        Long userId = resolveIdByEmail(authentication.getName());

        SuggestionAcquisition suggestion = new SuggestionAcquisition();
        suggestion.setUserId(userId);
        suggestion.setTitle(dto.title());
        suggestion.setAuthor(dto.author());
        // ISBN opcional: "" se guarda como null para no chocar con el @Pattern del DTO.
        String isbn = dto.isbn() == null || dto.isbn().isBlank() ? null : dto.isbn();
        suggestion.setIsbn(isbn);
        suggestion.setJustificacion(dto.justificacion());
        suggestion.setStatus(SuggestionAcquisition.PENDIENTE);

        return toDTO(suggestionRepo.save(suggestion));
    }
    /**
     * Lista en forma paginada las sugerencias propias del lector autenticado para su seguimiento.
     * Tolera el orden legacy {@code creadoEn} traduciéndolo al atributo real de la entidad.
     *
     * @param authentication identidad autenticada del lector cuyas sugerencias se consultan
     * @param pageable paginación, tamaño y orden solicitados por la vista de seguimiento
     * @return página de sugerencias del lector con su estado y revisor
     * @throws jakarta.persistence.EntityNotFoundException si el usuario autenticado ya no existe
     */
    @Transactional(readOnly = true)
    public Page<SuggestionAcquisitionResponseDTO> listOwns(Authentication authentication, Pageable pageable) {
        Long userId = resolveIdByEmail(authentication.getName());
        return suggestionRepo.findByUserId(userId, sortTolerante(pageable)).map(this::toDTO);
    }

    // Solo GERENTE/ADMIN llegan acá: listado sin filtrar por dueño.
    /**
     * Lista en forma paginada todas las sugerencias para la bandeja de GERENTE o ADMIN, con filtro
     * opcional por estado. Tolera el orden legacy {@code creadoEn} traduciéndolo al atributo real.
     *
     * @param status estado por el que se filtra, por ejemplo PENDIENTE; nulo o vacío trae todos
     * @param pageable paginación, tamaño y orden solicitados por la bandeja
     * @return página de sugerencias de todos los lectores con su estado y revisor
     */
    @Transactional(readOnly = true)
    public Page<SuggestionAcquisitionResponseDTO> listAll(String status, Pageable pageable) {
        Pageable tolerante = sortTolerante(pageable);
        if (status == null || status.isBlank()) {
            return suggestionRepo.findAll(tolerante).map(this::toDTO);
        }
        return suggestionRepo.findByStatus(status, tolerante).map(this::toDTO);
    }
    /**
     * Cambia el estado de una sugerencia (aprobada o rechazada) registrando al revisor autenticado.
     * Solo GERENTE o ADMIN llegan a este punto por la regla de la ruta que lo expone.
     *
     * @param id identificador de la sugerencia a dictaminar
     * @param freshStatus estado nuevo que reemplaza al anterior
     * @param authentication identidad autenticada del revisor que queda registrada en la sugerencia
     * @return la sugerencia con el estado nuevo y su revisor
     * @throws jakarta.persistence.EntityNotFoundException si la sugerencia o el revisor no existen
     */
    @Transactional
    public SuggestionAcquisitionResponseDTO changeStatus(Long id, String freshStatus, Authentication authentication) {
        SuggestionAcquisition suggestion = suggestionRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SUGERENCIA_NO_ENCONTRADA + id));

        Long revisorId = resolveIdByEmail(authentication.getName());
        suggestion.setStatus(freshStatus);
        suggestion.setRevisadoBy(revisorId);

        return toDTO(suggestionRepo.save(suggestion));
    }

    // ── Gestión por demanda: lo más pedido primero ──
    // El orden vive en el JPQL; el sort del Pageable se ignora a propósito.
    /**
     * Pagina las sugerencias agrupadas por título con su conteo de solicitudes, de lo más pedido
     * a lo menos pedido, para priorizar las compras. El orden vive en la consulta y el sort del
     * pageable se ignora a propósito.
     *
     * @param pageable página y tamaño solicitados por el reporte de demanda
     * @return página de grupos con título, autor, ISBN y cantidad de solicitudes
     */
    @Transactional(readOnly = true)
    public Page<SuggestionGroupedDTO> getMostRequested(Pageable pageable) {
        Pageable effective = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return suggestionRepo.findMostRequestedGrouped(effective);
    }
    /**
     * Devuelve todos los grupos de sugerencias por demanda sin paginar para el PDF de más pedidas.
     *
     * @return grupos con título, autor, ISBN y cantidad de solicitudes, de lo más pedido primero
     */
    @Transactional(readOnly = true)
    public List<SuggestionGroupedDTO> getMostRequestedList() {
        return suggestionRepo
                .findMostRequestedGrouped(PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
    }

    /**
     * Marca como APROBADA cada sugerencia PENDIENTE con el ISBN recién ingresado al catálogo.
     * Se invoca al crear un libro para cerrar el ciclo pedido-compra sin revisión manual una por una.
     *
     * @param isbn ISBN del libro dado de alta que confirma los pedidos pendientes
     * @param revisorId identificador del revisor a registrar; nulo deja el campo sin revisor
     * @return cantidad de sugerencias pendientes que pasaron a APROBADA
     */
    @Transactional
    public int confirmAcquisition(String isbn, Long revisorId) {
        List<SuggestionAcquisition> pending = suggestionRepo.findByIsbnAndStatus(isbn, SuggestionAcquisition.PENDIENTE);
        for (SuggestionAcquisition s : pending) {
            s.setStatus(SuggestionAcquisition.APROBADA);
            s.setRevisadoBy(revisorId);
            suggestionRepo.save(s);
        }
        return pending.size();
    }

    private Long resolveIdByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + email))
                .getId();
    }

    /** Versión pública para el controller (confirmar-adquisicion). */
    /**
     * Resuelve el identificador de un usuario por su correo para el endpoint de confirmar adquisición.
     * Puente público hacia la resolución interna usada al crear y revisar sugerencias.
     *
     * @param email correo del usuario a resolver
     * @return identificador del usuario dueño de ese correo
     * @throws jakarta.persistence.EntityNotFoundException si ningún usuario tiene ese correo
     */
    public Long resolveIdByEmailPublic(String email) {
        return resolveIdByEmail(email);
    }

    private SuggestionAcquisitionResponseDTO toDTO(SuggestionAcquisition s) {
        return new SuggestionAcquisitionResponseDTO(
                s.getId(),
                s.getUserId(),
                s.getTitle(),
                s.getAuthor(),
                s.getIsbn(),
                s.getJustificacion(),
                s.getStatus(),
                s.getRevisadoBy(),
                s.getCreated()
        );
    }
}
