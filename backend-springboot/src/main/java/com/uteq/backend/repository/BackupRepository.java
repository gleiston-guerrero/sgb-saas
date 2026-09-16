package com.uteq.backend.repository;

import com.uteq.backend.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.OffsetDateTime;

/**
 * CRUD de respaldos más listados por rango, estado y tipo.
 */
@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {

    /**
     * Lista todos los respaldos del más reciente al más antiguo.
     *
     * @return lista de respaldos ordenada por creación descendente
     */
    @Query("SELECT b FROM Backup b ORDER BY b.created DESC")
    List<Backup> findAllOrderByCreatedDesc();

    /**
     * Lista los respaldos creados dentro del rango dado.
     *
     * @param from inicio del rango
     * @param until fin del rango
     * @return lista de respaldos en el rango, del más reciente al más antiguo
     */
    @Query("SELECT b FROM Backup b WHERE b.created >= :from AND b.created <= :until ORDER BY b.created DESC")
    List<Backup> findByDateRange(@Param("from") OffsetDateTime from, @Param("until") OffsetDateTime until);

    /**
     * Lista los respaldos en el estado dado.
     *
     * @param status estado a filtrar
     * @return lista de respaldos en ese estado
     */
    @Query("SELECT b FROM Backup b WHERE b.status = :status ORDER BY b.created DESC")
    List<Backup> findByStatus(@Param("status") String status);

    /**
     * Lista los respaldos del tipo dado.
     *
     * @param type tipo a filtrar
     * @return lista de respaldos de ese tipo
     */
    @Query("SELECT b FROM Backup b WHERE b.type = :type ORDER BY b.created DESC")
    List<Backup> findByType(@Param("type") String type);


}