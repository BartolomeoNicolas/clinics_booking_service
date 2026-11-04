package com.bartolomeo.clinics.booking.reservation.application.service;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Aplica eventos a los procesos y traduce el "no aplica" en el error propio.
 *
 * <p><strong>Es el único lugar donde se convierte un vacío en excepción.</strong> El dominio
 * devuelve vacío cuando la transición no existe —porque el dominio no puede depender de la
 * capa de aplicación— y esta pieza es la que decide qué significa ese vacío: un error con
 * status y código estable.
 *
 * <p><strong>Está en la aplicación y no en el dominio a propósito.</strong> Que la regla de
 * qué transiciones existen viva en el dominio y que el error que sale de ella lo arme la
 * aplicación son dos cosas distintas: la primera es una regla de negocio y se puede probar
 * sola; la segunda es una decisión de transporte y necesita el código de error.
 */
@Component
public class ReservationTransitions {

    /**
     * Aplica un evento al proceso.
     *
     * @param process proceso en su estado actual
     * @param event   evento que ocurrió
     * @param now     instante del cambio
     * @return el proceso en el estado nuevo
     * @throws InvalidReservationTransitionException si esa pareja de estado y evento no tiene
     *                                                transición
     */
    public ReservationProcess apply(ReservationProcess process, ReservationEvent event, Instant now) {
        return apply(process, event, now, null);
    }

    /**
     * Aplica un evento al proceso, dejando escrito por qué si el evento es un fallo.
     *
     * @param process proceso en su estado actual
     * @param event   evento que ocurrió
     * @param now     instante del cambio
     * @param reason  motivo del fallo; solo se guarda si el evento es {@code FAILED}
     * @return el proceso en el estado nuevo
     * @throws InvalidReservationTransitionException si esa pareja de estado y evento no tiene
     *                                                transición
     */
    public ReservationProcess apply(
            ReservationProcess process, ReservationEvent event, Instant now, String reason) {

        return process.apply(event, now, reason)
                .orElseThrow(() -> new InvalidReservationTransitionException(
                        process.state(), event));
    }

    /**
     * Guarda el retén que creó la cátedra.
     *
     * <p>Es el mismo camino que un evento más, con los datos del retén encima: el estado
     * nuevo y los identificadores se escriben en el mismo paso, así que no hay forma de
     * tener un proceso con el retén guardado y el estado viejo.
     *
     * @param process         proceso en su estado actual
     * @param holdId          retén que creó la cátedra
     * @param catedraProcessId proceso que creó la cátedra
     * @param expiresAt       vencimiento que dio la cátedra
     * @param now             instante del cambio
     * @return el proceso con el retén
     * @throws InvalidReservationTransitionException si el estado actual no admitía el retén
     */
    public ReservationProcess holdCreated(
            ReservationProcess process,
            String holdId,
            String catedraProcessId,
            Instant expiresAt,
            Instant now) {

        return process.holdCreated(holdId, catedraProcessId, expiresAt, now)
                .orElseThrow(() -> new InvalidReservationTransitionException(
                        process.state(), ReservationEvent.HOLD_CREATED));
    }
}
