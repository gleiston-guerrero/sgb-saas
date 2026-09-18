package com.uteq.backend.config;

import com.uteq.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Fija app.current_user_id antes de cada servicio transaccional de escritura.
 * El trigger de auditoría lo lee y evita grabar usuario NULL.
 */
@Aspect
@Component
public class AuditAspect {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    /**
     * Constructor con el repositorio de usuarios.
     *
     * @param userRepository repositorio para resolver el id por correo
     */
    public AuditAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Intercepta cada método transaccional de escritura y fija {@code app.current_user_id}
     * con el id del usuario autenticado, para que el trigger de auditoría no grabe NULL.
     * Si no hay sesión o falla {@code set_config}, deja continuar al método original.
     *
     * @param pjp punto de corte del método interceptado
     * @param tx anotación transaccional que marca al método como escritura cuando no es de solo lectura
     * @return resultado del método original interceptado
     * @throws Throwable si el método interceptado falla
     */
    @Around("@annotation(tx)")
    public Object setCurrentUser(ProceedingJoinPoint pjp, org.springframework.transaction.annotation.Transactional tx) throws Throwable {
        if (!tx.readOnly()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                if (email != null && email.contains("@")) {
                    userRepository.findByEmail(email).ifPresent(u -> {
                        try {
                            // set_config() en vez de SET LOCAL: PostgreSQL/JDBC
                            // no admite bind parameters ($1) en sentencias
                            // utilitarias como SET LOCAL ("syntax error at
                            // near $1" -> 503, ver fix ea1847f). Al ser
                            // llamada a funcion, set_config si acepta :id.
                            entityManager.createNativeQuery(
                                            "SELECT set_config('app.current_user_id', CAST(:id AS text), true)")
                                    .setParameter("id", u.getId().toString())
                                    .getSingleResult();
                        } catch (Exception ignored) {
                            // best-effort: si falla set_config, la operación
                            // de negocio continúa y el trigger registra NULL
                        }
                    });
                }
            }
        }
        return pjp.proceed();
    }
}
