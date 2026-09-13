package com.uteq.backend.repository;

import com.uteq.backend.entity.Fine;
import com.uteq.backend.entity.Loan;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProcedureMappingContractTest {

    @Test
    void sideEffectRoutines_useProcedureAnnotationsRequiredByRubric() throws NoSuchMethodException {
        assertProcedure(LoanProcedureRepository.class, "spCreateLoanProcedure",
                "proc_crear_prestamo", Long.class, Long.class, Long.class, Integer.class);
        assertProcedure(LoanProcedureRepository.class, "spRegisterLoanReturn",
                "Prestamo.registrarDevolucion", Long.class);
        assertProcedure(FineProcedureRepository.class, "spPayFineProcedure",
                "Multa.pagarMulta", Long.class);
        assertProcedure(FineProcedureRepository.class, "spVoidFineProcedure",
                "Multa.anularMulta", Long.class, String.class, String.class);
    }

    /**
     * Desde V51 existe proc_expirar_reservaciones_vencidas (CREATE
     * PROCEDURE nativo que envuelve la función sp_expirar_reservaciones_vencidas),
     * así que esta rutina dejó de ser la excepción "es una FUNCTION, no se
     * puede invocar con CALL" -- ahora declara @Procedure como las demás
     * rutinas con efectos secundarios.
     */
    @Test
    void reservationExpiry_usesProcedureAnnotationSinceV51WrapperExists() throws NoSuchMethodException {
        Method method = ReservationProcedureRepository.class
                .getDeclaredMethod("spExpireReservationsVencidasProcedure");

        assertThat(method.isAnnotationPresent(Query.class))
                .as("spExpireReservationsVencidasProcedure must not be implemented as @Query")
                .isFalse();

        Procedure procedure = method.getAnnotation(Procedure.class);
        assertThat(procedure)
                .as("spExpireReservationsVencidasProcedure must declare @Procedure")
                .isNotNull();
        assertThat(procedure.procedureName()).isEqualTo("proc_expirar_reservaciones_vencidas");
    }

    @Test
    void namedStoredProcedureQueries_existForMultiOutRoutines() {
        Map<String, String> loanProcedures = namedProcedures(Loan.class);
        Map<String, String> fineProcedures = namedProcedures(Fine.class);

        assertThat(loanProcedures)
                .containsEntry("Prestamo.registrarDevolucion", "proc_registrar_devolucion");
        assertThat(fineProcedures)
                .containsEntry("Multa.pagarMulta", "proc_pagar_multa")
                .containsEntry("Multa.anularMulta", "proc_anular_multa");
    }

    @Test
    void tableReturningFunctions_remainNativeQueriesBecauseJpaProcedureDoesNotMapSetReturningFunctions() {
        Set<String> nativeFunctionMethods = Arrays.stream(LoanProcedureRepository.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Query.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(nativeFunctionMethods)
                .contains(
                        "fnListLoansActivesByUser",
                        "fnReportBooksMostLoaned",
                        "fnReportIndexDelinquency",
                        "fnReportUsageByPeriod",
                        "fnReportInventory",
                        "fnReportLoansOverdues",
                        "fnReportCategoriesDemanded"
                );
    }

    private void assertProcedure(
            Class<?> repositoryType,
            String methodName,
            String expectedProcedureReference,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = repositoryType.getDeclaredMethod(methodName, parameterTypes);
        assertThat(method.isAnnotationPresent(Query.class))
                .as("%s must not be implemented as @Query", methodName)
                .isFalse();
        Procedure procedure = method.getAnnotation(Procedure.class);
        assertThat(procedure)
                .as("%s must declare @Procedure", methodName)
                .isNotNull();
        assertThat(procedure.procedureName().isBlank() ? procedure.name() : procedure.procedureName())
                .isEqualTo(expectedProcedureReference);
    }

    private Map<String, String> namedProcedures(Class<?> entityType) {
        NamedStoredProcedureQuery single = entityType.getAnnotation(NamedStoredProcedureQuery.class);
        NamedStoredProcedureQueries multiple = entityType.getAnnotation(NamedStoredProcedureQueries.class);

        if (multiple != null) {
            return Arrays.stream(multiple.value())
                    .collect(Collectors.toMap(NamedStoredProcedureQuery::name, NamedStoredProcedureQuery::procedureName));
        }
        if (single != null) {
            return Map.of(single.name(), single.procedureName());
        }
        return Map.of();
    }
}
