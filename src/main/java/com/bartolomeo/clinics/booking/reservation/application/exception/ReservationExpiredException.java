package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
import java.time.Instant;

/**
 * El proceso venció antes de que el usuario mandara el teléfono.
 *
 * <p><strong>Es un conflicto, no un pedido mal hecho.</strong> El teléfono estaba bien
 * escrito: lo que no se puede es mandarlo, porque la ventana de la reserva ya se cerró. Se
 * traduce a 409 con el código estable {@code invalid-state-transition}, y el proceso queda
 * cerrado como vencido.
 *
 * <p>Publicarlo igual no sería un error de la cátedra: sería mandar un mensaje que llega
 * tarde y no va a ser atendido, o peor, que se atiende para un proceso que ya venció.
 */
public class ReservationExpiredException extends ApplicationException {

    public ReservationExpiredException(Instant expiresAt) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "El proceso de reserva venció el " + expiresAt
                        + " y ya no se puede completar.");
    }
}
