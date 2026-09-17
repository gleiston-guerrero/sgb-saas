package com.uteq.backend.service;

import com.uteq.backend.repository.BookRepository;
import com.uteq.backend.repository.ReservationRepository;
import com.uteq.backend.repository.StatusReservationRepository;
import com.uteq.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationsManagementServiceTest {

    @Mock UserRepository userRepo;
    @Mock ReservationRepository reservationRepo;
    @Mock StatusReservationRepository statusReservationRepo;
    @Mock BookRepository bookRepo;

    @InjectMocks ReservationsManagementService service;

    // Regresion: el sort debe ser el atributo JPA dateReservation
    // ("fechaReserva" no existe ni como propiedad ni como columna y
    // rompia GET /gestion/historial-reservaciones siempre).
    @Test
    void historyReservations_usaSortDateReservationValido() {
        given(userRepo.existsById(5L)).willReturn(true);
        given(reservationRepo.findByUserId(eq(5L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(Page.empty());

        var result = service.historyReservations(5L);

        assertThat(result).isEmpty();
        verify(reservationRepo).findByUserId(eq(5L),
                argThat((Pageable p) -> p.getSort().stream()
                        .anyMatch(o -> o.getProperty().equals("dateReservation")
                                && o.getDirection().isDescending())));
    }
}
