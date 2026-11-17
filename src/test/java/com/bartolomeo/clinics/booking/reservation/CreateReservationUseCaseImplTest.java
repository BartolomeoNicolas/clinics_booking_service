package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldExpiredException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationAlreadyConfirmedException;
import com.bartolomeo.clinics.booking.catedra.application.exception.SlotAlreadyHeldException;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentConfirmation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.exception.MissingIdempotencyKeyException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationHoldUnreconcilableException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.application.usecases.CreateReservationUseCaseImpl;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.CreateReservationUseCase;
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
import org.mockito.InOrder;

/**
 * El arranque de una reserva: qué se guarda, en qué orden y qué se reconcilia.
 *
 * <p>Tests unitarios puros con los tres puertos simulados y un reloj fijo. Lo que se prueba
 * acá es el comportamiento que el issue exige —el proceso se guarda antes de la primera
 * llamada, un timeout se reconcilia antes de repetir, la vigencia es la de la cátedra— y
 * ninguna de esas reglas necesita una base ni una cátedra de verdad para estar probadas.
 *
 * <p><strong>El reloj fijo es lo que hace verificable la vigencia.</strong> Con un
 * {@code Instant.now()} en el medio, un retén "vencido" depende de la hora de la máquina.
 */
@DisplayName("el caso de uso de iniciar una reserva")
class CreateReservationUseCaseImplTest {

    private static final String LOGIN = "nbartolomeo";
    private static final Long USER_ID = 42L;
    private static final String PACIENTE = "user-42";
    private static final long PROFESIONAL = 15L;
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);
    private static final String CLAVE = "clave-de-solicitud-1";

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    /** El vencimiento que dio la cátedra: diez minutos de plazo. */
    private static final Instant VENCE = AHORA.plusSeconds(600);

    private final ReservationProcessRepository repository = mock(ReservationProcessRepository.class);
    private final CatedraAppointmentsPort catedra = mock(CatedraAppointmentsPort.class);
    private final GetUserUseCase getUser = mock(GetUserUseCase.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final CreateReservationUseCase useCase =
            new CreateReservationUseCaseImpl(
                    repository, catedra, getUser, new ReservationTransitions(), clock);

    // ---------------------------------------------------------------- helpers

    private static User usuario() {
        return User.builder()
                .id(USER_ID)
                .login(LOGIN)
                .firstName("Nicolás")
                .lastName("Bartolomeo")
                .build();
    }

    private static ReservationData datos() {
        return new ReservationData(PROFESIONAL, DIA, HORA, CLAVE);
    }

    private static AppointmentHold reten() {
        return new AppointmentHold("hold-6c0c2f41", "process-a92b47d0", "HELD", VENCE);
    }

    private static AppointmentConfirmation confirmacion() {
        return new AppointmentConfirmation("process-a92b47d0", "WAITING_FOR_PHONE", VENCE);
    }

    /** El proceso como queda guardado, con el identificador que le pone la base. */
    private static ReservationProcess guardado(ReservationProcess proceso) {
        return new ReservationProcess(
                7L,
                proceso.userId(),
                proceso.externalPatientId(),
                proceso.professionalId(),
                proceso.date(),
                proceso.startTime(),
                proceso.state(),
                proceso.holdId(),
                proceso.catedraProcessId(),
                proceso.requestEventId(),
                proceso.expiresAt(),
                proceso.requestKey(),
                proceso.failureReason(),
                proceso.createdAt(),
                proceso.updatedAt());
    }

    @BeforeEach
    void preparar() {
        when(getUser.findByLogin(LOGIN)).thenReturn(Optional.of(usuario()));
        when(repository.findByRequestKey(USER_ID, CLAVE)).thenReturn(Optional.empty());
        when(repository.save(any(ReservationProcess.class)))
                .thenAnswer(call -> guardado(call.getArgument(0)));
    }

    private void laCatedraAceptaElTurno() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE)).thenReturn(reten());
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(confirmacion());
    }

    // ----------------------------------------------- el proceso se guarda primero

    @Test
    @DisplayName("el proceso se guarda antes de la primera llamada a la catedra")
    void guardaElProcesoAntesDeLlamar() {
        laCatedraAceptaElTurno();

        useCase.create(LOGIN, datos());

        InOrder orden = inOrder(repository, catedra);
        orden.verify(repository).save(any(ReservationProcess.class));
        orden.verify(catedra).createHold(PROFESIONAL, DIA, HORA, PACIENTE);
    }

    @Test
    @DisplayName("el proceso queda asociado al usuario que lo inicio")
    void elProcesoQuedaAsociadoAlUsuario() {
        laCatedraAceptaElTurno();

        useCase.create(LOGIN, datos());

        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        ReservationProcess primero = captor.getAllValues().getFirst();
        assertThat(primero.userId()).isEqualTo(USER_ID);
        // El identificador del paciente sale del usuario, no de lo que pidio el cliente.
        assertThat(primero.externalPatientId()).isEqualTo(PACIENTE);
        assertThat(primero.state()).isEqualTo(ReservationState.INITIATED);
    }

    @Test
    @DisplayName("guarda el reten, el proceso de la catedra y la vigencia que dio")
    void guardaLoQueDioLaCatedra() {
        laCatedraAceptaElTurno();

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(proceso.catedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(proceso.expiresAt()).isEqualTo(VENCE);
        assertThat(proceso.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
    }

    @Test
    @DisplayName("confirma el reten con el nombre del usuario y el identificador del paciente")
    void confirmaConLosDatosDelUsuario() {
        laCatedraAceptaElTurno();

        useCase.create(LOGIN, datos());

        verify(catedra).confirmHold("hold-6c0c2f41", PACIENTE, "Nicolás", "Bartolomeo");
    }

    // --------------------------------------------------- idempotencia por clave

    @Test
    @DisplayName("una solicitud repetida con la misma clave no crea un segundo proceso")
    void laMismaClaveNoCreaUnSegundoProceso() {
        ReservationProcess yaHecho = new ReservationProcess(
                7L, USER_ID, PACIENTE, PROFESIONAL, DIA, HORA,
                ReservationState.WAITING_FOR_PHONE, "hold-6c0c2f41", "process-a92b47d0",
                "request-event-8a31", VENCE, CLAVE, null, AHORA, AHORA);
        when(repository.findByRequestKey(USER_ID, CLAVE)).thenReturn(Optional.of(yaHecho));

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.id()).isEqualTo(7L);
        verify(repository, never()).save(any(ReservationProcess.class));
        verifyNoInteractions(catedra);
    }

    @Test
    @DisplayName("una solicitud repetida de un proceso a medio hacer lo continua")
    void laMismaClaveContinuaElProcesoQueQuedoAMedias() {
        ReservationProcess aMedias = new ReservationProcess(
                7L, USER_ID, PACIENTE, PROFESIONAL, DIA, HORA,
                ReservationState.INITIATED, null, null, null, null,
                CLAVE, null, AHORA, AHORA);
        when(repository.findByRequestKey(USER_ID, CLAVE)).thenReturn(Optional.of(aMedias));
        laCatedraAceptaElTurno();

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
        // No se crea un proceso nuevo: se sigue el que ya existia.
        verify(repository, never())
                .save(argThat(p -> p.state() == ReservationState.INITIATED));
    }

    @Test
    @DisplayName("sin clave de idempotencia la solicitud se rechaza antes de tocar nada")
    void sinClaveNoSeEmpieza() {
        var error = catchThrowableOfType(
                MissingIdempotencyKeyException.class,
                () -> useCase.create(LOGIN, new ReservationData(PROFESIONAL, DIA, HORA, "  ")));

        assertThat(error).isNotNull();
        verifyNoInteractions(repository, catedra);
    }

    // ------------------------------------------- un timeout se reconcilia antes de repetir

    @Test
    @DisplayName("ante un timeout al pedir el reten consulta el estado antes de repetir")
    void anteUnTimeoutConsultaElEstado() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE))
                .thenThrow(new CatedraUnavailableException("se perdio la respuesta"))
                .thenReturn(reten());
        when(catedra.getOccupancies(PROFESIONAL, DIA, DIA)).thenReturn(List.of());
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(confirmacion());

        useCase.create(LOGIN, datos());

        InOrder orden = inOrder(catedra);
        orden.verify(catedra).createHold(PROFESIONAL, DIA, HORA, PACIENTE);
        orden.verify(catedra).getOccupancies(PROFESIONAL, DIA, DIA);
        orden.verify(catedra).createHold(PROFESIONAL, DIA, HORA, PACIENTE);
    }

    @Test
    @DisplayName("si el turno ya quedo tomado en la catedra no se repite el reten")
    void siElTurnoQuedoTomadoNoSeRepite() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE))
                .thenThrow(new CatedraUnavailableException("se perdio la respuesta"));
        when(catedra.getOccupancies(PROFESIONAL, DIA, DIA)).thenReturn(List.of(
                new Occupancy(DIA, HORA, OccupancyStatus.HELD, VENCE)));

        var error = catchThrowableOfType(
                ReservationHoldUnreconcilableException.class,
                () -> useCase.create(LOGIN, datos()));

        assertThat(error).isNotNull();
        verify(catedra, times(1)).createHold(anyLong(), any(), any(), anyString());
    }

    @Test
    @DisplayName("ante un timeout al confirmar consulta si la reserva ya existe")
    void anteUnTimeoutAlConfirmarConsultaLasReservas() {
        laCatedraAceptaElTurno();
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new CatedraUnavailableException("se perdio la respuesta"));
        when(catedra.listAppointments(any())).thenReturn(List.of(
                new CatedraAppointment(
                        8251L, "process-a92b47d0", PACIENTE, DIA, HORA, HORA.plusMinutes(30),
                        PROFESIONAL, "Ana", "Gomez", "Nicolás", "Bartolomeo", null,
                        CatedraReservationStatus.PHONE_PENDING, AHORA, null, null, null)));

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
        verify(catedra, times(1)).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("si la reserva todavia no existe reintenta la confirmacion una sola vez")
    void siLaReservaNoExisteReintentaLaConfirmacion() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE)).thenReturn(reten());
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new CatedraUnavailableException("se perdio la respuesta"))
                .thenReturn(confirmacion());
        when(catedra.listAppointments(any())).thenReturn(List.of());

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
        verify(catedra, times(2)).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("una confirmacion que ya habia pasado no es un error")
    void unaConfirmacionYaHechaNoEsUnError() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE)).thenReturn(reten());
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ReservationAlreadyConfirmedException("process-a92b47d0"));

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
    }

    // ----------------------------------------------------- la vigencia es la de la catedra

    @Test
    @DisplayName("la vigencia que queda es la de la catedra y no se recalcula")
    void laVigenciaEsLaDeLaCatedra() {
        laCatedraAceptaElTurno();

        ReservationProcess proceso = useCase.create(LOGIN, datos());

        assertThat(proceso.expiresAt()).isEqualTo(VENCE);
        // Ni el reloj del servicio ni el guardado posterior cambian el vencimiento: una copia
        // local no extiende el TTL del otro lado.
        assertThat(proceso.expiresAt()).isEqualTo(VENCE);
        assertThat(proceso.updatedAt()).isEqualTo(AHORA);
    }

    // ------------------------------------------------------- los fallos cierran el proceso

    @Test
    @DisplayName("un turno ya tomado deja el proceso fallido y el error sigue al cliente")
    void unTurnoYaTomadoDejaElProcesoFallido() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE))
                .thenThrow(new SlotAlreadyHeldException("El turno ya está tomado."));

        var error = catchThrowableOfType(SlotAlreadyHeldException.class, () -> useCase.create(LOGIN, datos()));

        assertThat(error).isNotNull();
        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().state()).isEqualTo(ReservationState.FAILED);
    }

    @Test
    @DisplayName("un reten que ya no existe deja el proceso vencido")
    void unRetenQueNoExisteDejaElProcesoVencido() {
        ReservationProcess aMedias = new ReservationProcess(
                7L, USER_ID, PACIENTE, PROFESIONAL, DIA, HORA,
                ReservationState.HOLD_CREATED, "hold-6c0c2f41", "process-a92b47d0",
                "request-event-8a31", VENCE, CLAVE, null, AHORA, AHORA);
        when(repository.findByRequestKey(USER_ID, CLAVE)).thenReturn(Optional.of(aMedias));
        when(catedra.confirmHold(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new HoldExpiredException("hold-6c0c2f41"));

        var error = catchThrowableOfType(HoldExpiredException.class, () -> useCase.create(LOGIN, datos()));

        assertThat(error).isNotNull();
        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().state()).isEqualTo(ReservationState.EXPIRED);
    }

    @Test
    @DisplayName("si la catedra no esta el proceso no se da por fallido")
    void siLaCatedraNoEstaElProcesoNoFalla() {
        when(catedra.createHold(PROFESIONAL, DIA, HORA, PACIENTE))
                .thenThrow(new CatedraUnavailableException("no responde"))
                .thenThrow(new CatedraUnavailableException("no responde"));

        var error = catchThrowableOfType(CatedraUnavailableException.class, () -> useCase.create(LOGIN, datos()));

        assertThat(error).isNotNull();
        ArgumentCaptor<ReservationProcess> captor =
                ArgumentCaptor.forClass(ReservationProcess.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().state())
                .isNotIn(ReservationState.FAILED, ReservationState.EXPIRED);
    }

    @Test
    @DisplayName("el login del token es el usuario del proceso")
    void elLoginEsElUsuarioDelProceso() {
        laCatedraAceptaElTurno();

        useCase.create(LOGIN, datos());

        verify(getUser).findByLogin(eq(LOGIN));
    }
}
