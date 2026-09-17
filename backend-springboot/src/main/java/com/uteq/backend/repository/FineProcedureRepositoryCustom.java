package com.uteq.backend.repository;

import com.uteq.backend.repository.projection.RecentPaymentProjection;
import com.uteq.backend.repository.projection.SummaryFinancialFinesProjection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fragmento custom para stored procedures de multas (P5: StoredProcedureQuery
 * posicional) y reportes tabulares reimplementados sin SQL nativo.
 */
public interface FineProcedureRepositoryCustom {
    Map<String, Object> spPayFineProcedure(Long fineId);
    Map<String, Object> spVoidFineProcedure(Long fineId, String reason, String roleExecutor);
    Map<String, Object> spPaymentParcialFine(Long fineId, BigDecimal amountPaid);
    SummaryFinancialFinesProjection fnReportSummaryFinancial(OffsetDateTime from, OffsetDateTime until);
    List<RecentPaymentProjection> fnPaymentsRecientes(Integer limit);
}
