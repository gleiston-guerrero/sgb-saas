package com.uteq.backend.repository;

import com.uteq.backend.entity.RegistrationDamageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationDamageDetailRepository extends JpaRepository<RegistrationDamageDetail, Long> {

    /** Lista los detalles del registro de daño dado. */
    List<RegistrationDamageDetail> findByRegistrationDamageId(Long registrationDamageId);
}
