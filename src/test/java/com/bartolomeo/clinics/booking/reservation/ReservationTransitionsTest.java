package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Aplicar un evento a un proceso.
 *
 * <p>La tabla ya está probada en {@link ReservationStateMachineTest}; acá lo que se prueba es
 * lo que pasa alrededor: que el proceso devuelva el estado nuevo con todo lo demás intacto,
 * que un evento que no aplica no lo toque, y que la capa de aplicación convierta ese "no
 * aplica" en el error propio con su código estable.
 */
@DisplayName("aplicar eventos a un proceso de reserva")
class ReservationTransitionsTest {

    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private final ReservationTransitions transitions = new ReservationTransitions();

    private ReservationProcess proceso(ReservationState estado) {
        return new ReservationProcess(
                7L, 42L, "user-42", 15L, DIA, HORA, estado,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                AHORA.plusSeconds(600),
                "clave-1", null, AHORA, AHORA);
    }

    @Test
    @DisplayName("aplica el evento y deja el proceso en el estado nuevo")
    void aplicaElEvento() {
        ReservationProcess actualizado = transitions.apply(
                proceso(ReservationState.WAITING_FOR_PHONE),
                ReservationEvent.PHONE_SUBMITTED,
                AHORA);

        assertThat(actualizado.state()).isEqualTo(ReservationState.PHONE_SUBMITTED);
        assertThat(actualizado.updatedAt()).isEqualTo(AHORA);
        // Todo lo demas se conserva: el turno, el retén y la vigencia no se tocan.
        assertThat(actualizado.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(actualizado.catedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(actualizado.expiresAt()).isEqualTo(AHORA.plusSeconds(600));
        assertThat(actualizado.date()).isEqualTo(DIA);
    }

    @Test
    @DisplayName("un evento que no aplica deja el proceso como estaba")
    void unEventoQueNoAplicaNoTocaElProceso() {
        ReservationProcess original = proceso(ReservationState.WAITING_FOR_PHONE);

        assertThat(original.apply(ReservationEvent.HOLD_CREATED, AHORA)).isEmpty();
        assertThat(original.state()).isEqualTo(ReservationState.WAITING_FOR_PHONE);
    }

    @Test
    @DisplayName("la aplicacion rechaza con el error propio una transicion invalida")
    void rechazaConElErrorPropio() {
        var error = catchThrowableOfType(
                InvalidReservationTransitionException.class,
                () -> transitions.apply(
                        proceso(ReservationState.CANCELLED),
                        ReservationEvent.CONFIRMED,
                        AHORA));

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode().getCode()).isEqualTo("invalid-state-transition");
        assertThat(error.getMessage()).contains("CANCELLED").contains("CONFIRMED");
    }

    @Test
    @DisplayName("un fallo deja escrito por que y los demas fallos no lo tocan")
    void elMotivoQuedaSoloEnElFallo() {
        ReservationProcess fallido = transitions.apply(
                proceso(ReservationState.WAITING_FOR_PHONE),
                ReservationEvent.FAILED,
                AHORA,
                "El turno ya estaba tomado.");

        assertThat(fallido.state()).isEqualTo(ReservationState.FAILED);
        assertThat(fallido.failureReason()).isEqualTo("El turno ya estaba tomado.");

        ReservationProcess confirmado = transitions.apply(
                proceso(ReservationState.WAITING_FOR_PHONE),
                ReservationEvent.CONFIRMED,
                AHORA,
                "motivo que no corresponde");

        assertThat(confirmado.state()).isEqualTo(ReservationState.CONFIRMED);
        assertThat(confirmado.failureReason()).isNull();
    }

    @Test
    @DisplayName("guardar el reten escribe el retén y el estado en el mismo paso")
    void elRetenSeGuardaConSuEstado() {
        ReservationProcess conReten = transitions.holdCreated(
                proceso(ReservationState.INITIATED),
                "hold-nuevo",
                "process-nuevo",
                AHORA.plusSeconds(300),
                AHORA);

        assertThat(conReten.state()).isEqualTo(ReservationState.HOLD_CREATED);
        assertThat(conReten.holdId()).isEqualTo("hold-nuevo");
        assertThat(conReten.catedraProcessId()).isEqualTo("process-nuevo");
        assertThat(conReten.expiresAt()).isEqualTo(AHORA.plusSeconds(300));
    }
}
