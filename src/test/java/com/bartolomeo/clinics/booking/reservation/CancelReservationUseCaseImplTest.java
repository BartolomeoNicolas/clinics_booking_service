package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.application.usecases.CancelReservationUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cancelar una reserva.
 *
 * <p>Lo que importa acá es el aislamiento y la idempotencia: que nadie cancele lo de otro y
 * que cancelar dos veces no rompa nada.
 */
@DisplayName("el caso de uso que cancela una reserva")
class CancelReservationUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final CatedraAppointmentsPort catedra = mock(CatedraAppointmentsPort.class);
    private final GetUserUseCase getUser = mock(GetUserUseCase.class);

    private final CancelReservationUseCaseImpl useCase = new CancelReservationUseCaseImpl(
            repository,
            catedra,
            getUser,
            new ReservationTransitions(),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private static ReservationProcess proceso(Long userId, ReservationState estado) {
        return new ReservationProcess(
                7L, userId, "user-" + userId, 15L, DIA, HORA, estado,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                AHORA.plusSeconds(600), "clave-1", null, AHORA, AHORA);
    }

    @BeforeEach
    void preparar() {
        when(getUser.findByLogin("nbartolomeo")).thenReturn(Optional.of(
                User.builder().id(42L).login("nbartolomeo").build()));
        when(repository.save(any(ReservationProcess.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("cancela en la catedra y deja el proceso local cancelado")
    void cancela() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(proceso(42L, ReservationState.CONFIRMED)));

        ReservationProcess cancelado =
                useCase.cancel(7L, "nbartolomeo", "Cambio de planes");

        assertThat(cancelado.state()).isEqualTo(ReservationState.CANCELLED);
        verify(catedra).cancelAppointment("process-a92b47d0", "Cambio de planes");
        verify(repository).save(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("cancelar dos veces devuelve el estado actual y no repite nada")
    void cancelarDosVecesDevuelveElEstadoActual() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(proceso(42L, ReservationState.CANCELLED)));

        ReservationProcess cancelado =
                useCase.cancel(7L, "nbartolomeo", null);

        assertThat(cancelado.state()).isEqualTo(ReservationState.CANCELLED);
        // La segunda cancelacion no vuelve a molestar a la catedra.
        verify(catedra, never()).cancelAppointment(anyString(), any());
    }

    @Test
    @DisplayName("un proceso de otro usuario no existe")
    void unProcesoDeOtroUsuarioNoExiste() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(proceso(999L, ReservationState.CONFIRMED)));

        var error = catchThrowableOfType(
                ReservationProcessNotFoundException.class,
                () -> useCase.cancel(7L, "nbartolomeo", null));

        assertThat(error).isNotNull();
        verify(catedra, never()).cancelAppointment(anyString(), any());
    }

    @Test
    @DisplayName("un proceso que ya vencio no se cancela")
    void unProcesoVencidoNoSeCancela() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(proceso(42L, ReservationState.EXPIRED)));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.cancel(7L, "nbartolomeo", null));

        assertThat(error).isNotNull();
        verify(catedra, never()).cancelAppointment(anyString(), any());
    }

    @Test
    @DisplayName("un proceso que todavia no tiene nada en la catedra no se cancela")
    void unProcesoSinRetenNoSeCancela() {
        ReservationProcess sinReten = new ReservationProcess(
                7L, 42L, "user-42", 15L, DIA, HORA, ReservationState.INITIATED,
                null, null, null, AHORA.plusSeconds(600), "clave-1", null, AHORA, AHORA);
        when(repository.findById(7L)).thenReturn(Optional.of(sinReten));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.cancel(7L, "nbartolomeo", null));

        assertThat(error).isNotNull();
        verify(catedra, never()).cancelAppointment(eq("process-a92b47d0"), any());
    }
}
