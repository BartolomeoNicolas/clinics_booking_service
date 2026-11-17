package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.exception.UnknownReservationProcessException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.application.usecases.ProcessReservationEventUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEventEnvelope;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ProcessedEventRepository;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationRepository;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Aplicar un evento de la cátedra a su proceso.
 *
 * <p>Lo que importa acá es la idempotencia y el aislamiento: la entrega de Kafka es al
 * menos una vez, así que el mismo mensaje puede llegar dos veces, y los mensajes de todos los
 * alumnos pasan por el mismo topic, así que puede llegar un evento que no es de ningún
 * proceso nuestro.
 */
@DisplayName("el caso de uso que aplica un evento de la catedra")
class ProcessReservationEventUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final ProcessReservationEventUseCase useCase = new ProcessReservationEventUseCaseImpl(
            repository, reservations, processedEvents, new ReservationTransitions(), clock);

    private static ReservationProcess proceso(ReservationState estado) {
        return new ReservationProcess(
                7L, 42L, "user-42", 15L, DIA, HORA, estado,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                AHORA.plusSeconds(600),
                "clave-1", null, AHORA, AHORA);
    }

    private static ReservationEventEnvelope envelope(String eventId, ReservationEvent evento) {
        return new ReservationEventEnvelope(
                eventId, evento, "process-a92b47d0", 1, AHORA, 8251L, null);
    }

    @BeforeEach
    void preparar() {
        when(processedEvents.existsByEventId(anyString())).thenReturn(false);
        when(repository.findByCatedraProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso(ReservationState.PHONE_SUBMITTED)));
        when(repository.save(any(ReservationProcess.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("aplica el evento al proceso que le corresponde")
    void aplicaElEvento() {
        useCase.process(envelope("evento-1", ReservationEvent.CONFIRMED));

        verify(repository).save(any(ReservationProcess.class));
        verify(processedEvents).save("evento-1", 1);
    }

    @Test
    @DisplayName("el mismo eventId dos veces no repite el efecto")
    void elMismoEventIdNoRepiteElEfecto() {
        when(processedEvents.existsByEventId("evento-2")).thenReturn(true);

        useCase.process(envelope("evento-2", ReservationEvent.CONFIRMED));

        verify(repository, never()).save(any(ReservationProcess.class));
        verify(repository, never()).findByCatedraProcessId(anyString());
        verify(processedEvents, never()).save(anyString(), anyInt());
    }

    @Test
    @DisplayName("el efecto se aplica antes de marcar el evento como procesado")
    void elEfectoSeAplicaAntesDeMarcarElEvento() {
        useCase.process(envelope("evento-3", ReservationEvent.CONFIRMED));

        InOrder orden = inOrder(repository, processedEvents);
        orden.verify(repository).save(any(ReservationProcess.class));
        orden.verify(processedEvents).save("evento-3", 1);
    }

    @Test
    @DisplayName("un evento de un proceso ajeno no se aplica")
    void unEventoDeUnProcesoAjenoNoSeAplica() {
        when(repository.findByCatedraProcessId("process-ajeno"))
                .thenReturn(Optional.empty());

        var error = catchThrowableOfType(
                UnknownReservationProcessException.class,
                () -> useCase.process(new ReservationEventEnvelope(
                        "evento-4", ReservationEvent.CONFIRMED, "process-ajeno", 1, AHORA,
                        8251L, null)));

        assertThat(error).isNotNull();
        verify(repository, never()).save(any(ReservationProcess.class));
        verify(processedEvents, never()).save(anyString(), anyInt());
    }

    @Test
    @DisplayName("un evento que no aplica al estado del proceso se rechaza")
    void unEventoQueNoAplicaSeRechaza() {
        when(repository.findByCatedraProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso(ReservationState.CANCELLED)));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.process(envelope("evento-5", ReservationEvent.CONFIRMED)));

        assertThat(error).isNotNull();
        verify(repository, never()).save(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("un evento tardio no reabre un proceso cerrado")
    void unEventoTardioNoReabreUnProcesoCerrado() {
        when(repository.findByCatedraProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso(ReservationState.EXPIRED)));

        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> useCase.process(envelope("evento-6", ReservationEvent.CONFIRMED)));

        assertThat(error).isNotNull();
        verify(repository, never()).save(any(ReservationProcess.class));
        verify(reservations, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("una confirmacion deja la reserva guardada")
    void unaConfirmacionDejaLaReservaGuardada() {
        when(reservations.findByProcessId(7L)).thenReturn(Optional.empty());

        useCase.process(envelope("evento-7", ReservationEvent.CONFIRMED));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(captor.capture());

        Reservation reserva = captor.getValue();
        assertThat(reserva.catedraReservationId()).isEqualTo(8251L);
        assertThat(reserva.processId()).isEqualTo(7L);
        assertThat(reserva.userId()).isEqualTo(42L);
        assertThat(reserva.professionalId()).isEqualTo(15L);
        assertThat(reserva.date()).isEqualTo(DIA);
        assertThat(reserva.startTime()).isEqualTo(HORA);
        assertThat(reserva.status()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("una confirmacion repetida no deja una segunda reserva")
    void unaConfirmacionRepetidaNoDejaUnaSegundaReserva() {
        when(reservations.findByProcessId(7L)).thenReturn(Optional.of(
                new Reservation(
                        1L, 8251L, 7L, 42L, 15L, DIA, HORA, null, "CONFIRMED",
                        null, null, null, null, AHORA)));

        useCase.process(envelope("evento-8", ReservationEvent.CONFIRMED));

        verify(reservations, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("una cancelacion cambia el estado de la reserva")
    void unaCancelacionCambiaElEstadoDeLaReserva() {
        when(repository.findByCatedraProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso(ReservationState.CONFIRMED)));
        when(reservations.findByProcessId(7L)).thenReturn(Optional.of(
                new Reservation(
                        1L, 8251L, 7L, 42L, 15L, DIA, HORA, null, "CONFIRMED",
                        null, null, null, null, AHORA)));

        useCase.process(envelope("evento-9", ReservationEvent.CANCELLED));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("CANCELLED");
        assertThat(captor.getValue().createdAt()).isEqualTo(AHORA);
    }
}
