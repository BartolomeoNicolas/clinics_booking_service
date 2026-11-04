package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidPhoneNumberException;
import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationExpiredException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.application.usecases.SubmitPhoneUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneSubmission;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.SubmitPhoneUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Mandar el teléfono.
 *
 * <p>Lo que se prueba acá es lo que el issue pide: que un teléfono inválido se rechace sin
 * publicar nada, que un reenvío después de un rechazo use un identificador nuevo y
 * referencie el pedido vigente, y que nunca se publique fuera de la ventana.
 */
@DisplayName("el caso de uso que manda el telefono")
class SubmitPhoneUseCaseImplTest {

    private static final String LOGIN = "nbartolomeo";
    private static final Long USER_ID = 42L;
    private static final long PROCESS_ID = 7L;
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final Instant VENCE = AHORA.plusSeconds(600);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final PhoneSubmissionPublisher publisher = mock(PhoneSubmissionPublisher.class);
    private final GetUserUseCase getUser = mock(GetUserUseCase.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final SubmitPhoneUseCase useCase = new SubmitPhoneUseCaseImpl(
            repository, publisher, getUser, new ReservationTransitions(), clock);

    private static ReservationProcess proceso(ReservationState estado) {
        return new ReservationProcess(
                PROCESS_ID, USER_ID, "user-42", 15L, DIA, HORA, estado,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                VENCE, "clave-1", null, AHORA, AHORA);
    }

    @BeforeEach
    void preparar() {
        when(getUser.findByLogin(LOGIN)).thenReturn(Optional.of(User.builder()
                .id(USER_ID)
                .login(LOGIN)
                .firstName("Nicolás")
                .lastName("Bartolomeo")
                .build()));
        when(repository.findById(PROCESS_ID))
                .thenReturn(Optional.of(proceso(ReservationState.WAITING_FOR_PHONE)));
        when(repository.save(any(ReservationProcess.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private PhoneSubmission publicado() {
        ArgumentCaptor<PhoneSubmission> captor = ArgumentCaptor.forClass(PhoneSubmission.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("publica el telefono normalizado y deja el proceso esperando el resultado")
    void publicaElTelefono() {
        ReservationProcess actualizado = useCase.submitPhone(
                PROCESS_ID, LOGIN, "+54 9 261-555-1234");

        PhoneSubmission envio = publicado();
        assertThat(envio.phoneNumber()).isEqualTo("+5492615551234");
        assertThat(envio.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(envio.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(envio.requestEventId()).isEqualTo("request-event-8a31");
        assertThat(actualizado.state()).isEqualTo(ReservationState.PHONE_SUBMITTED);
    }

    @Test
    @DisplayName("un telefono invalido se rechaza y no se publica nada")
    void unTelefonoInvalidoNoPublica() {
        var error = catchThrowableOfType(
                InvalidPhoneNumberException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "no soy un telefono"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());
        verify(repository, never()).save(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("un proceso de otro usuario no existe")
    void unProcesoAjenoNoExiste() {
        when(repository.findById(PROCESS_ID))
                .thenReturn(Optional.of(new ReservationProcess(
                        PROCESS_ID, 999L, "user-999", 15L, DIA, HORA,
                        ReservationState.WAITING_FOR_PHONE, "hold-6c0c2f41", "process-a92b47d0",
                        "request-event-8a31", VENCE, "clave-1", null, AHORA, AHORA)));

        var error = catchThrowableOfType(
                ReservationProcessNotFoundException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("un proceso que no existe tampoco")
    void unProcesoInexistenteNoExiste() {
        when(repository.findById(PROCESS_ID)).thenReturn(Optional.empty());

        var error = catchThrowableOfType(
                ReservationProcessNotFoundException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("vencido el proceso no se publica nada y queda vencido")
    void vencidoElProcesoNoSePublica() {
        when(repository.findById(PROCESS_ID)).thenReturn(Optional.of(
                new ReservationProcess(
                        PROCESS_ID, USER_ID, "user-42", 15L, DIA, HORA,
                        ReservationState.WAITING_FOR_PHONE, "hold-6c0c2f41", "process-a92b47d0",
                        "request-event-8a31", AHORA.minusSeconds(1), "clave-1", null, AHORA, AHORA)));

        var error = catchThrowableOfType(
                ReservationExpiredException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());

        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().state()).isEqualTo(ReservationState.EXPIRED);
    }

    @Test
    @DisplayName("un proceso que no espera el telefono no lo manda")
    void unProcesoQueNoEsperaNoManda() {
        when(repository.findById(PROCESS_ID))
                .thenReturn(Optional.of(proceso(ReservationState.CONFIRMED)));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("un proceso sin pedido vigente no tiene a que responder")
    void sinPedidoVigenteNoHayQueResponder() {
        when(repository.findById(PROCESS_ID)).thenReturn(Optional.of(
                new ReservationProcess(
                        PROCESS_ID, USER_ID, "user-42", 15L, DIA, HORA,
                        ReservationState.WAITING_FOR_PHONE, "hold-6c0c2f41", "process-a92b47d0",
                        null, VENCE, "clave-1", null, AHORA, AHORA)));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234"));

        assertThat(error).isNotNull();
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("un reenvio despues de un rechazo usa un evento nuevo y el pedido vigente")
    void unReenvioUsaUnEventoNuevo() {
        when(repository.findById(PROCESS_ID))
                .thenReturn(Optional.of(proceso(ReservationState.REJECTED)));

        useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234");
        useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234");

        ArgumentCaptor<PhoneSubmission> captor = ArgumentCaptor.forClass(PhoneSubmission.class);
        verify(publisher, times(2)).publish(captor.capture());

        List<PhoneSubmission> envios = captor.getAllValues();
        assertThat(envios.get(0).eventId()).isNotEqualTo(envios.get(1).eventId());
        assertThat(envios.get(0).requestEventId()).isEqualTo("request-event-8a31");
        assertThat(envios.get(1).requestEventId()).isEqualTo("request-event-8a31");
    }

    @Test
    @DisplayName("un rechazo deja el proceso reintentable")
    void unRechazoDejaElProcesoReintentable() {
        when(repository.findById(PROCESS_ID))
                .thenReturn(Optional.of(proceso(ReservationState.REJECTED)));

        ReservationProcess actualizado = useCase.submitPhone(
                PROCESS_ID, LOGIN, "+5492615551234");

        assertThat(actualizado.state()).isEqualTo(ReservationState.PHONE_SUBMITTED);
    }

    @Test
    @DisplayName("el mensaje sale con el proceso de la catedra como clave")
    void elMensajeSaleConElProcesoComoClave() {
        useCase.submitPhone(PROCESS_ID, LOGIN, "+5492615551234");

        PhoneSubmission envio = publicado();
        assertThat(envio.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(envio.eventId()).isNotBlank();
        verify(repository).save(any(ReservationProcess.class));
        verify(getUser).findByLogin(anyString());
        verify(repository, atLeastOnce()).findById(anyLong());
    }
}
