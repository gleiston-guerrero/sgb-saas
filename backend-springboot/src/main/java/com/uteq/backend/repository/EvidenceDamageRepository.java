package com.uteq.backend.repository;

import com.uteq.backend.entity.EvidenceDamage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceDamageRepository extends JpaRepository<EvidenceDamage, Long> {

    /** Lista las evidencias del registro de daño dado. */
    List<EvidenceDamage> findByRegistrationDamageId(Long registrationDamageId);
}
