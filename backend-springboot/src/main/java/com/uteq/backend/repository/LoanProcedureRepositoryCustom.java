package com.uteq.backend.repository;

import com.uteq.backend.repository.projection.BookMostLoanedDetailedProjection;
import com.uteq.backend.repository.projection.BookMostLoanedProjection;
import com.uteq.backend.repository.projection.LoanActiveBaseProjection;
import com.uteq.backend.repository.projection.ReportCategoriesDemandedProjection;
import com.uteq.backend.repository.projection.ReportDelinquencyProjection;
import com.uteq.backend.repository.projection.ReportInventoryProjection;
import com.uteq.backend.repository.projection.ReportOverduesProjection;
import com.uteq.backend.repository.projection.ReportUsageByPeriodProjection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fragmento custom para stored procedures de prestamos (P5:
 * StoredProcedureQuery posicional) y reportes tabulares reimplementados
 * sin SQL nativo (Criteria/JPQL + cómputo Java con semántica idéntica a
 * cada función fn_*; las funciones quedan en BD para psql directo).
 */
public interface LoanProcedureRepositoryCustom {
    Long spCreateLoanProcedure(Long userId, Long bookId, Long librarianId, Integer daysLoan);
    Map<String, Object> spRegisterLoanReturn(Long loanId);

    List<LoanActiveBaseProjection> fnListLoansActivesByUser(Long userId);

    List<BookMostLoanedProjection> fnReportBooksMostLoaned(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until);

    List<ReportDelinquencyProjection> fnReportIndexDelinquency(Integer maxLimit);
    List<ReportDelinquencyProjection> fnReportIndexDelinquencyPaginated(
            Integer maxLimit, int limit, int offset);
    long countReportIndexDelinquency(Integer maxLimit);

    List<ReportUsageByPeriodProjection> fnReportUsageByPeriod(String granularidad,
            OffsetDateTime from, OffsetDateTime until);
    List<ReportUsageByPeriodProjection> fnReportUsageByPeriodPaginated(String granularidad,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset);
    long countReportUsageByPeriod(String granularidad, OffsetDateTime from, OffsetDateTime until);

    List<BookMostLoanedDetailedProjection> fnReportBooksMostLoanedDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId);
    List<BookMostLoanedDetailedProjection> fnReportBooksDetailedPaginated(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId, int limit, int offset);
    long countReportBooksDetailed(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, Integer categoryId);

    List<ReportInventoryProjection> fnReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location);
    List<ReportInventoryProjection> fnReportInventoryPaginated(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location,
            int limit, int offset);
    long countReportInventory(Integer categoryId, String statusStock,
            String busqueda, Integer publisherId, Integer supplierId, Integer statusBookId,
            Integer languageId, Short yearFrom, Short yearUntil, Short stockTotalMin,
            Short stockTotalMax, Short stockDispMin, Short stockDispMax, String location);

    List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin, String busqueda,
            Integer daysAtrasoMax);

    default List<ReportOverduesProjection> fnReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return fnReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin,
            String busqueda, Integer daysAtrasoMax, int limit, int offset);

    default List<ReportOverduesProjection> fnReportLoansOverduesPaginated(Integer daysAtrasoMin,
            String busqueda, int limit, int offset) {
        return fnReportLoansOverduesPaginated(daysAtrasoMin, busqueda, null, limit, offset);
    }

    long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda, Integer daysAtrasoMax);

    default long countReportLoansOverdues(Integer daysAtrasoMin, String busqueda) {
        return countReportLoansOverdues(daysAtrasoMin, busqueda, null);
    }

    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemanded(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until);
    List<ReportCategoriesDemandedProjection> fnReportCategoriesDemandedPaginated(Integer maxLimit,
            OffsetDateTime from, OffsetDateTime until, int limit, int offset);
    long countReportCategoriesDemanded(Integer maxLimit, OffsetDateTime from, OffsetDateTime until);
}
