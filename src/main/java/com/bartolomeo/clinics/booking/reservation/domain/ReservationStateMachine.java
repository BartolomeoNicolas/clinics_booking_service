package com.bartolomeo.clinics.booking.reservation.domain;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * La tabla de transiciones del proceso de reserva.
 *
 * <p><strong>Es la única fuente de verdad de qué se puede hacer con un proceso.</strong> Toda
 * pareja de estado y evento tiene una fila o no la tiene, y no tenerla significa que el
 * evento no aplica. La tabla está escrita una sola vez, acá, y los tests la recorren entera
 * para que ningún par se quede sin decidir.
 *
 * <p><strong>Un estado terminal no retrocede.</strong> Los estados cerrados —cancelado,
 * vencido, inválido, fallido— no tienen ninguna fila de salida: un evento tardío que llega
 * sobre un proceso ya cerrado no lo reabre, no lo mueve y no lo deja medio despierto. La
 * única excepción a "cerrado" es la confirmación, que sí se puede cancelar después: cancelar
 * una reserva confirmada no es retroceder, es el final que le corresponde.
 *
 * <p><strong>Lo que la cátedra dice al final manda.</strong> Si la confirmación o el rechazo
 * llegan a un proceso que no está exactamente donde se los esperaba —porque un mensaje se
 * perdió o llegó en otro orden— el proceso converge al estado que la cátedra dice. Por eso
 * la tabla admite la confirmación desde cualquiera de los estados en espera, y no solo desde
 * el último.
 *
 * <p><strong>Acá no se lanza ninguna excepción.</strong> El dominio no depende de la capa de
 * aplicación (R1), así que una transición que no existe se devuelve como vacío y es la capa
 * de aplicación la que la convierte en el error propio.
 */
public final class ReservationStateMachine {

    private static final Map<ReservationState, Map<ReservationEvent, ReservationState>> TABLA =
            construir();

    private ReservationStateMachine() {
    }

    /**
     * El estado al que lleva un evento aplicado a un estado.
     *
     * @param state estado en que está el proceso
     * @param event evento que ocurrió
     * @return el estado nuevo, o vacío si esa pareja no tiene transición
     */
    public static Optional<ReservationState> nextState(ReservationState state, ReservationEvent event) {
        if (state == null || event == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                TABLA.getOrDefault(state, Map.of()).get(event));
    }

    /**
     * Si el proceso está cerrado, es decir, si ningún evento lo mueve más.
     *
     * <p>Un proceso cerrado no se reabre. Es la invariante que evita que un evento tardío
     * deshaga un resultado final.
     *
     * @param state estado del proceso
     * @return {@code true} si no hay ninguna transición de salida
     */
    public static boolean isClosed(ReservationState state) {
        return state != null && TABLA.getOrDefault(state, Map.of()).isEmpty();
    }

    /**
     * Los estados a los que se puede llegar desde alguno de los que no están cerrados.
     *
     * @return los estados que tienen al menos una transición de salida
     */
    public static Map<ReservationState, Map<ReservationEvent, ReservationState>> transitions() {
        return Map.copyOf(TABLA);
    }

    private static Map<ReservationState, Map<ReservationEvent, ReservationState>> construir() {
        Map<ReservationState, Map<ReservationEvent, ReservationState>> tabla =
                new EnumMap<>(ReservationState.class);

        // Todavía no se le pidió nada a la cátedra.
        tabla.put(ReservationState.INITIATED, transiciones(
                Map.entry(ReservationEvent.HOLD_CREATED, ReservationState.HOLD_CREATED),
                Map.entry(ReservationEvent.EXPIRED, ReservationState.EXPIRED),
                Map.entry(ReservationEvent.FAILED, ReservationState.FAILED)));

        // Hay retén, pero la reserva todavía no existe.
        tabla.put(ReservationState.HOLD_CREATED, transiciones(
                Map.entry(ReservationEvent.INITIAL_CONFIRMATION, ReservationState.WAITING_FOR_PHONE),
                Map.entry(ReservationEvent.CANCELLED, ReservationState.CANCELLED),
                Map.entry(ReservationEvent.EXPIRED, ReservationState.EXPIRED),
                Map.entry(ReservationEvent.INVALID, ReservationState.INVALID),
                Map.entry(ReservationEvent.FAILED, ReservationState.FAILED)));

        // La reserva existe y espera el teléfono.
        tabla.put(ReservationState.WAITING_FOR_PHONE, transiciones(
                Map.entry(ReservationEvent.PHONE_SUBMITTED, ReservationState.PHONE_SUBMITTED),
                Map.entry(ReservationEvent.PHONE_REJECTED, ReservationState.REJECTED),
                Map.entry(ReservationEvent.CONFIRMED, ReservationState.CONFIRMED),
                Map.entry(ReservationEvent.CANCELLED, ReservationState.CANCELLED),
                Map.entry(ReservationEvent.EXPIRED, ReservationState.EXPIRED),
                Map.entry(ReservationEvent.INVALID, ReservationState.INVALID),
                Map.entry(ReservationEvent.FAILED, ReservationState.FAILED)));

        // El teléfono se mandó y todavía no se sabe el resultado.
        tabla.put(ReservationState.PHONE_SUBMITTED, transiciones(
                Map.entry(ReservationEvent.PHONE_REJECTED, ReservationState.REJECTED),
                Map.entry(ReservationEvent.CONFIRMED, ReservationState.CONFIRMED),
                Map.entry(ReservationEvent.CANCELLED, ReservationState.CANCELLED),
                Map.entry(ReservationEvent.EXPIRED, ReservationState.EXPIRED),
                Map.entry(ReservationEvent.INVALID, ReservationState.INVALID),
                Map.entry(ReservationEvent.FAILED, ReservationState.FAILED)));

        // Rechazado y reintentable: se puede volver a mandar el teléfono.
        tabla.put(ReservationState.REJECTED, transiciones(
                Map.entry(ReservationEvent.PHONE_SUBMITTED, ReservationState.PHONE_SUBMITTED),
                Map.entry(ReservationEvent.PHONE_REJECTED, ReservationState.REJECTED),
                Map.entry(ReservationEvent.CONFIRMED, ReservationState.CONFIRMED),
                Map.entry(ReservationEvent.CANCELLED, ReservationState.CANCELLED),
                Map.entry(ReservationEvent.EXPIRED, ReservationState.EXPIRED),
                Map.entry(ReservationEvent.INVALID, ReservationState.INVALID),
                Map.entry(ReservationEvent.FAILED, ReservationState.FAILED)));

        // Confirmada: el único camino que le queda es la cancelación.
        tabla.put(ReservationState.CONFIRMED, transiciones(
                Map.entry(ReservationEvent.CANCELLED, ReservationState.CANCELLED)));

        // Cancelado, vencido, inválido y fallido están cerrados: no tienen salida.
        tabla.put(ReservationState.CANCELLED, Map.of());
        tabla.put(ReservationState.EXPIRED, Map.of());
        tabla.put(ReservationState.INVALID, Map.of());
        tabla.put(ReservationState.FAILED, Map.of());

        return Map.copyOf(tabla);
    }

    @SafeVarargs
    private static Map<ReservationEvent, ReservationState> transiciones(
            Map.Entry<ReservationEvent, ReservationState>... entradas) {
        return Map.ofEntries(entradas);
    }
}
