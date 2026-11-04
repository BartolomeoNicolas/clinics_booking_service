package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bartolomeo.clinics.booking.reservation.domain.ReservationStateMachine;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La tabla de transiciones,(pair por pair.
 *
 * <p><strong>La tabla del test está escrita aparte de la del código.</strong> Si se copiaran
 * una de la otra, el test no probaría nada: copiaría la implementación. Acá la tabla está
 * escrita como la especificación que es, y el test la contrasta con la implementación.
 *
 * <p><strong>Y además se recorre entera.</strong> El segundo test no mira solo las parejas
 * permitidas: recorre los noventa pares posibles —diez estados por nueve eventos— y exige
 * que los que no están en la tabla se rechacen. Una pareja que nadie decidió queda
 * prohibida por omisión, y ese es el riesgo real de una tabla: que un día se agregue un
 * evento y se olvide decidir qué pasa con los estados viejos.
 */
@DisplayName("la maquina de estados del proceso de reserva")
class ReservationStateMachineTest {

    /** Una fila de la tabla: de dónde se sale, con qué evento y a dónde se llega. */
    private record Transicion(ReservationState desde, ReservationEvent evento, ReservationState hacia) {
    }

    /** La tabla, escrita como especificación y no como copia de la implementación. */
    private static final List<Transicion> PERMITIDAS = List.of(
            // Todavia no se le pidio nada a la catedra.
            new Transicion(ReservationState.INITIATED, ReservationEvent.HOLD_CREATED,
                    ReservationState.HOLD_CREATED),
            new Transicion(ReservationState.INITIATED, ReservationEvent.EXPIRED,
                    ReservationState.EXPIRED),
            new Transicion(ReservationState.INITIATED, ReservationEvent.FAILED,
                    ReservationState.FAILED),

            // Hay reten, pero la reserva todavia no existe.
            new Transicion(ReservationState.HOLD_CREATED, ReservationEvent.INITIAL_CONFIRMATION,
                    ReservationState.WAITING_FOR_PHONE),
            new Transicion(ReservationState.HOLD_CREATED, ReservationEvent.CANCELLED,
                    ReservationState.CANCELLED),
            new Transicion(ReservationState.HOLD_CREATED, ReservationEvent.EXPIRED,
                    ReservationState.EXPIRED),
            new Transicion(ReservationState.HOLD_CREATED, ReservationEvent.INVALID,
                    ReservationState.INVALID),
            new Transicion(ReservationState.HOLD_CREATED, ReservationEvent.FAILED,
                    ReservationState.FAILED),

            // La reserva existe y espera el telefono.
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.PHONE_SUBMITTED,
                    ReservationState.PHONE_SUBMITTED),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.PHONE_REJECTED,
                    ReservationState.REJECTED),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.CONFIRMED,
                    ReservationState.CONFIRMED),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.CANCELLED,
                    ReservationState.CANCELLED),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.EXPIRED,
                    ReservationState.EXPIRED),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.INVALID,
                    ReservationState.INVALID),
            new Transicion(ReservationState.WAITING_FOR_PHONE, ReservationEvent.FAILED,
                    ReservationState.FAILED),

            // El telefono se mando y todavia no se sabe el resultado.
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.PHONE_REJECTED,
                    ReservationState.REJECTED),
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.CONFIRMED,
                    ReservationState.CONFIRMED),
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.CANCELLED,
                    ReservationState.CANCELLED),
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.EXPIRED,
                    ReservationState.EXPIRED),
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.INVALID,
                    ReservationState.INVALID),
            new Transicion(ReservationState.PHONE_SUBMITTED, ReservationEvent.FAILED,
                    ReservationState.FAILED),

            // Rechazado y reintentable.
            new Transicion(ReservationState.REJECTED, ReservationEvent.PHONE_SUBMITTED,
                    ReservationState.PHONE_SUBMITTED),
            new Transicion(ReservationState.REJECTED, ReservationEvent.PHONE_REJECTED,
                    ReservationState.REJECTED),
            new Transicion(ReservationState.REJECTED, ReservationEvent.CONFIRMED,
                    ReservationState.CONFIRMED),
            new Transicion(ReservationState.REJECTED, ReservationEvent.CANCELLED,
                    ReservationState.CANCELLED),
            new Transicion(ReservationState.REJECTED, ReservationEvent.EXPIRED,
                    ReservationState.EXPIRED),
            new Transicion(ReservationState.REJECTED, ReservationEvent.INVALID,
                    ReservationState.INVALID),
            new Transicion(ReservationState.REJECTED, ReservationEvent.FAILED,
                    ReservationState.FAILED),

            // Confirmada: el unico camino que le queda es la cancelacion.
            new Transicion(ReservationState.CONFIRMED, ReservationEvent.CANCELLED,
                    ReservationState.CANCELLED));

    private static boolean permitida(ReservationState estado, ReservationEvent evento) {
        return PERMITIDAS.stream()
                .anyMatch(t -> t.desde() == estado && t.evento() == evento);
    }

    @Test
    @DisplayName("cada pareja permitida lleva al estado que dice la tabla")
    void cadaParejaPermitidaLlevaAlEstadoEsperado() {
        for (Transicion transicion : PERMITIDAS) {
            Optional<ReservationState> resultado =
                    ReservationStateMachine.nextState(transicion.desde(), transicion.evento());

            assertThat(resultado)
                    .as("de %s con %s", transicion.desde(), transicion.evento())
                    .contains(transicion.hacia());
        }
    }

    @Test
    @DisplayName("toda pareja que no esta en la tabla se rechaza")
    void todaParejaQueNoEstaEnLaTablaSeRechaza() {
        for (ReservationState estado : ReservationState.values()) {
            for (ReservationEvent evento : ReservationEvent.values()) {
                Optional<ReservationState> resultado =
                        ReservationStateMachine.nextState(estado, evento);

                if (permitida(estado, evento)) {
                    assertThat(resultado)
                            .as("la tabla declara %s + %s como valida", estado, evento)
                            .isPresent();
                } else {
                    assertThat(resultado)
                            .as("%s + %s no esta en la tabla y tiene que rechazarse",
                                    estado, evento)
                            .isEmpty();
                }
            }
        }
    }

    @Test
    @DisplayName("los estados cerrados no tienen salida: ningun evento los reabre")
    void losEstadosCerradosNoSeReabren() {
        List<ReservationState> cerrados = List.of(
                ReservationState.CANCELLED,
                ReservationState.EXPIRED,
                ReservationState.INVALID,
                ReservationState.FAILED);

        for (ReservationState estado : cerrados) {
            assertThat(ReservationStateMachine.isClosed(estado))
                    .as("%s esta cerrado", estado)
                    .isTrue();

            for (ReservationEvent evento : ReservationEvent.values()) {
                assertThat(ReservationStateMachine.nextState(estado, evento))
                        .as("%s + %s no puede mover un proceso cerrado", estado, evento)
                        .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("de la confirmacion solo se sale por la cancelacion")
    void deLaConfirmacionSoloSeSalePorLaCancelacion() {
        List<Transicion> salidas = PERMITIDAS.stream()
                .filter(t -> t.desde() == ReservationState.CONFIRMED)
                .toList();

        assertThat(salidas).hasSize(1);
        assertThat(salidas.getFirst().hacia()).isEqualTo(ReservationState.CANCELLED);
    }

    @Test
    @DisplayName("un estado que no esta en la tabla no tiene ninguna salida")
    void unEstadoFueraDeLaTablaNoTieneSalida() {
        assertThat(ReservationStateMachine.nextState(null, ReservationEvent.CONFIRMED)).isEmpty();
        assertThat(ReservationStateMachine.nextState(ReservationState.CONFIRMED, null)).isEmpty();
        assertThat(ReservationStateMachine.isClosed(null)).isFalse();
    }
}
