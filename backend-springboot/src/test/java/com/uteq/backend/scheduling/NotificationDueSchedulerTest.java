package com.uteq.backend.scheduling;

import com.uteq.backend.entity.StatusLoan;
import com.uteq.backend.entity.Loan;
import com.uteq.backend.repository.StatusLoanRepository;
import com.uteq.backend.repository.LoanRepository;
import com.uteq.backend.service.ConfigurationSystemService;
import com.uteq.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDueSchedulerTest {

    @Mock private LoanRepository loanRepo;
    @Mock private StatusLoanRepository statusLoanRepo;
    @Mock private NotificationService notificationService;
    @Mock private ConfigurationSystemService configurationSystemService;

    private NotificationDueScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationDueScheduler(loanRepo, statusLoanRepo, notificationService, configurationSystemService);

        // Lenient: los tests de seed parcial (config/catalogo ausente)
        // redefinen estos stubs y dejarían los de aquí sin usar.
        lenient().when(configurationSystemService.getIntegerValue("dias_anticipacion_vencimiento")).thenReturn(1);
        lenient().when(statusLoanRepo.findByName("ACTIVO")).thenReturn(Optional.of(status(1, "ACTIVO")));
        lenient().when(statusLoanRepo.findByName("RENOVADO")).thenReturn(Optional.of(status(2, "RENOVADO")));
    }

    // Los ids de ACTIVO/RENOVADO deben resolverse por nombre (no
    // hardcodeados) y pasarse tal cual a la consulta de la ventana.
    @Test
    void notifyNextsAExpire_consultaWithStatusesVigentesResueltos() {
        given(loanRepo.findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                anyList(), any(), any())).willReturn(List.of());

        scheduler.notifyNextToExpire();

        verify(loanRepo).findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                eq(List.of(1, 2)), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    // Cada préstamo dentro de la ventana dispara exactamente una llamada a
    // NotificacionService -- la dedup real vive ahí, no en el scheduler.
    @Test
    void notifyNextsAExpire_delegaEveryLoanNotificationService() {
        Loan p1 = loanWithId(1L);
        Loan p2 = loanWithId(2L);
        given(loanRepo.findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                anyList(), any(), any())).willReturn(List.of(p1, p2));

        scheduler.notifyNextToExpire();

        verify(notificationService).generateDueAlert(p1);
        verify(notificationService).generateDueAlert(p2);
        verify(notificationService, times(2)).generateDueAlert(any());
    }

    @Test
    void notifyNextsAExpire_withoutLoansVentana_notLlamaANotificationService() {
        given(loanRepo.findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                anyList(), any(), any())).willReturn(List.of());

        scheduler.notifyNextToExpire();

        verify(notificationService, never()).generateDueAlert(any());
    }

    // Seed parcial: sin clave de configuración el ciclo se omite sin
    // lanzar (antes EntityNotFoundException → ERROR cada minuto en log).
    @Test
    void notifyNextsAExpire_withoutConfigKey_omiteCicloWithoutLanzar() {
        given(configurationSystemService.getIntegerValue("dias_anticipacion_vencimiento"))
                .willThrow(new jakarta.persistence.EntityNotFoundException("CLAVE_NO_ENCONTRADA"));

        assertDoesNotThrow(() -> scheduler.notifyNextToExpire());

        verify(loanRepo, never()).findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                anyList(), any(), any());
        verify(notificationService, never()).generateDueAlert(any());
    }

    // Seed parcial: con RENOVADO ausente el ciclo usa solo ACTIVO en vez
    // de fallar (antes IllegalStateException → ERROR cada minuto en log).
    @Test
    void notifyNextsAExpire_withCatalogoParcial_consultaSoloWithPresentes() {
        given(statusLoanRepo.findByName("RENOVADO")).willReturn(Optional.empty());
        given(loanRepo.findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                anyList(), any(), any())).willReturn(List.of());

        assertDoesNotThrow(() -> scheduler.notifyNextToExpire());

        verify(loanRepo).findByStatusLoanIdInAndDateLoanReturnEstimadaBetween(
                eq(List.of(1)), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    private StatusLoan status(Integer id, String name) {
        StatusLoan status = new StatusLoan();
        status.setId(id);
        status.setName(name);
        return status;
    }

    private Loan loanWithId(Long id) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setUserId(1L);
        loan.setBookId(2L);
        loan.setDateLoanReturnEstimada(OffsetDateTime.now().plusMinutes(10));
        return loan;
    }
}
