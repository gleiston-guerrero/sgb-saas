package com.uteq.backend.repository;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fragmento custom para stored procedures de multas.
 */
public interface FineProcedureRepositoryCustom {
    Map<String, Object> spPayFineProcedure(Long fineId);
    Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor);
    Map<String, Object> spPaymentParcialFine(Long fineId, BigDecimal amountPaid);
}
