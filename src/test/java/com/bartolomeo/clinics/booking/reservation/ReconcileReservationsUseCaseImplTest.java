package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationRegistrar;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.application.usecases.ReconcileReservationsUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La reconciliación: qué pasa con un proceso que quedó a medias.
 *
 * <p>Lo que se prueba es la decisión de fondo —qué se cierra, qué se toca y qué se deja
 * como estaba— porque esa decisión es la que evita que un reinicio deje procesos imposibles.
 */
@DisplayName("la reconciliacion de procesos pendientes")
class ReconcileReservationsUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final CatedraAppointmentsPort catedra = mock(CatedraAppointmentsPort.class);
    private final ReservationRegistrar registrar = mock(ReservationRegistrar.class);

    private final ReconcileReservationsUseCaseImpl useCase = new ReconcileReservationsUseCaseImpl(
            repository,
            catedra,
            new ReservationTransitions(),
            registrar,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private static ReservationProcess proceso(ReservationState estado, Instant expiresAt) {
        return new ReservationProcess(
                7L, 42L, "user-42", 15L, DIA, HORA, estado,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                expiresAt, "clave-1", null, AHORA, AHORA);
    }

    private static CatedraAppointment reserva(CatedraReservationStatus estado) {
        return new CatedraAppointment(
                8251L, "process-a92b47d0", "user-42", DIA, HORA, HORA.plusMinutes(30),
                15L, "Ana", "Gomez", "Nicolás", "Bartolomeo", "+5492615551234",
                estado, AHORA, AHORA, null, null);
    }

    @BeforeEach
    void preparar() {
        when(catedra.listAppointments(any())).thenReturn(List.of());
    }

    private void dejaAbiertos(ReservationProcess... procesos) {
        when(repository.findOpen()).thenReturn(List.of(procesos));
    }

    private ReservationState estadoGuardado() {
        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository).save(captor.capture());
        return captor.getValue().state();
    }

    @Test
    @DisplayName("un proceso vencido se cierra sin preguntar nada a la catedra")
    void unProcesoVencidoSeCierra() {
        dejaAbiertos(proceso(ReservationState.WAITING_FOR_PHONE, AHORA.minusSeconds(1)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isEqualTo(1);
        assertThat(estadoGuardado()).isEqualTo(ReservationState.EXPIRED);
        verifyNoInteractions(catedra);
    }

    @Test
    @DisplayName("un proceso que esperaba una reserva que la catedra no tiene se cae")
    void unProcesoSinReservaSeCae() {
        dejaAbiertos(proceso(ReservationState.WAITING_FOR_PHONE, AHORA.plusSeconds(600)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isEqualTo(1);
        assertThat(estadoGuardado()).isEqualTo(ReservationState.EXPIRED);
    }

    @Test
    @DisplayName("un proceso que solo alcanzo a tomar el reten no se toca")
    void unProcesoQueSoloTomoElRetenNoSeToca() {
        // La catedra no lista los retenes, solo las reservas: que no aparezca no prueba nada.
        dejaAbiertos(proceso(ReservationState.HOLD_CREATED, AHORA.plusSeconds(600)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isZero();
        verify(repository, never()).save(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("una reserva confirmada en la catedra confirma el proceso local")
    void unaReservaConfirmadaConfirmaElProceso() {
        when(catedra.listAppointments(any()))
                .thenReturn(List.of(reserva(CatedraReservationStatus.CONFIRMED)));
        dejaAbiertos(proceso(ReservationState.WAITING_FOR_PHONE, AHORA.plusSeconds(600)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isEqualTo(1);
        assertThat(estadoGuardado()).isEqualTo(ReservationState.CONFIRMED);
        verify(registrar).registraConfirmacion(any(ReservationProcess.class), anyLong());
    }

    @Test
    @DisplayName("una reserva cancelada en la catedra cancela el proceso local")
    void unaReservaCanceladaCancelaElProceso() {
        when(catedra.listAppointments(any()))
                .thenReturn(List.of(reserva(CatedraReservationStatus.CANCELLED)));
        dejaAbiertos(proceso(ReservationState.CONFIRMED, AHORA.plusSeconds(600)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isEqualTo(1);
        assertThat(estadoGuardado()).isEqualTo(ReservationState.CANCELLED);
        verify(registrar).registraCancelacion(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("una reserva que sigue esperando el telefono no mueve nada")
    void unaReservaPendienteNoMueveNada() {
        when(catedra.listAppointments(any()))
                .thenReturn(List.of(reserva(CatedraReservationStatus.PHONE_PENDING)));
        dejaAbiertos(proceso(ReservationState.PHONE_SUBMITTED, AHORA.plusSeconds(600)));

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isZero();
        verify(repository, never()).save(any(ReservationProcess.class));
    }

    @Test
    @DisplayName("una falla de la catedra no corta el barrido")
    void unaFallaNoCortaElBarrido() {
        when(catedra.listAppointments(any()))
                .thenThrow(new RuntimeException("la catedra no responde"))
                .thenReturn(List.of(reserva(CatedraReservationStatus.CONFIRMED)));
        dejaAbiertos(
                proceso(ReservationState.WAITING_FOR_PHONE, AHORA.plusSeconds(600)),
                proceso(ReservationState.WAITING_FOR_PHONE, AHORA.plusSeconds(600)));

        useCase.reconcilePending();

        // El segundo proceso se revisa igual.
        verify(catedra, times(2)).listAppointments(any());
    }

    @Test
    @DisplayName("la reserva se busca por el paciente y por el dia del turno")
    void laReservaSeBuscaPorElPacienteYElDia() {
        dejaAbiertos(proceso(ReservationState.WAITING_FOR_PHONE, AHORA.plusSeconds(600)));

        useCase.reconcilePending();

        ArgumentCaptor<AppointmentsQuery> captor = ArgumentCaptor.forClass(AppointmentsQuery.class);
        verify(catedra).listAppointments(captor.capture());

        AppointmentsQuery query = captor.getValue();
        assertThat(query.externalPatientId()).isEqualTo("user-42");
        assertThat(query.from()).isEqualTo(DIA);
        assertThat(query.to()).isEqualTo(DIA);
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("un proceso que ya no esta abierto no se revisa")
    void unProcesoCerradoNoSeRevisa() {
        when(repository.findOpen()).thenReturn(List.of());

        int convergidos = useCase.reconcilePending();

        assertThat(convergidos).isZero();
        verifyNoInteractions(catedra);
        verify(repository, never()).save(any(ReservationProcess.class));
    }
}
