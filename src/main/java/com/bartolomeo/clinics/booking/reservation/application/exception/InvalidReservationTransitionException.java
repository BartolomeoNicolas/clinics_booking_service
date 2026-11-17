package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El evento no aplica al estado en que está el proceso.
 *
 * <p><strong>Es un conflicto, no un pedido mal hecho.</strong> El pedido está bien formado:
 * lo que no se puede es aplicarlo ahí. Cancelar una reserva que ya está cancelada, mandar
 * el teléfono de un proceso que ya venció, aplicar un evento que llegó tarde sobre un
 * proceso que ya se cerró: en los tres casos la respuesta es 409 con el código estable
 * {@code invalid-state-transition}, que es lo que le dice al cliente que su pedido era
 * válido pero que el mundo ya estaba en otro estado.
 *
 * <p><strong>Lo crea la capa de aplicación y no el dominio.</strong> El dominio no depende
 * de la capa de aplicación, así que devuelve vacío cuando la transición no existe y es acá
 * donde ese vacío se convierte en error.
 */
public class InvalidReservationTransitionException extends ApplicationException {

    public InvalidReservationTransitionException(ReservationState state, ReservationEvent event) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "El evento " + event + " no aplica a un proceso que está en estado " + state + ".");
    }
}
