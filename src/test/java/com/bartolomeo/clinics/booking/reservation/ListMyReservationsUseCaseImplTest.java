package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.reservation.application.usecases.ListMyReservationsUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.PageResult;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El listado de las reservas del usuario.
 *
 * <p>Lo que se prueba es que el filtro por usuario no se puede esquivar: no hay forma de
 * pedir las reservas de otro, porque el usuario sale del token y la consulta se arma con él.
 */
@DisplayName("el caso de uso que lista las reservas del usuario")
class ListMyReservationsUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final GetUserUseCase getUser = mock(GetUserUseCase.class);

    private final ListMyReservationsUseCaseImpl useCase =
            new ListMyReservationsUseCaseImpl(repository, getUser);

    private static ReservationProcess proceso(long id) {
        return new ReservationProcess(
                id, 42L, "user-42", 15L, DIA, HORA, ReservationState.CONFIRMED,
                "hold-6c0c2f41", "process-" + id, "request-event-8a31",
                AHORA.plusSeconds(600), "clave-" + id, null, AHORA, AHORA);
    }

    @BeforeEach
    void preparar() {
        when(getUser.findByLogin("nbartolomeo")).thenReturn(Optional.of(
                User.builder().id(42L).login("nbartolomeo").build()));
        when(repository.findByUserId(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(proceso(7L), proceso(8L)));
        when(repository.countByUserId(anyLong())).thenReturn(2L);
    }

    @Test
    @DisplayName("la consulta se arma con el usuario del token")
    void laConsultaSeArmaConElUsuarioDelToken() {
        useCase.list("nbartolomeo", 0, 20);

        verify(repository).findByUserId(42L, 0, 20);
        verify(repository).countByUserId(42L);
    }

    @Test
    @DisplayName("devuelve la pagina con su total y si hay mas")
    void devuelveLaPaginaConSuTotal() {
        PageResult<ReservationProcess> pagina = useCase.list("nbartolomeo", 0, 20);

        assertThat(pagina.content()).hasSize(2);
        assertThat(pagina.total()).isEqualTo(2L);
        assertThat(pagina.page()).isZero();
        assertThat(pagina.hasMore()).isFalse();
    }

    @Test
    @DisplayName("una pagina negativa o de tamano cero se corrige")
    void losParametrosAbsurdosSeCorrigen() {
        PageResult<ReservationProcess> pagina = useCase.list("nbartolomeo", -3, 0);

        assertThat(pagina.page()).isZero();
        assertThat(pagina.size()).isEqualTo(1);
        verify(repository).findByUserId(42L, 0, 1);
    }

    @Test
    @DisplayName("el tamano de pagina tiene un tope")
    void elTamanoDePaginaTieneUnTope() {
        useCase.list("nbartolomeo", 0, 5000);

        verify(repository).findByUserId(42L, 0, 100);
    }
}
