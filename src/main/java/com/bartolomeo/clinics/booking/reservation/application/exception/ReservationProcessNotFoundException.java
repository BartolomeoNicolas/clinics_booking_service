package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El proceso no existe o no es de quien pregunta.
 *
 * <p><strong>Las dos cosas se responden igual.</strong> Confirmarle a alguien que el proceso
 * existe, pero que es de otro, ya es filtrarle información: por eso el mismo error dice
 * "not found" para los dos casos. Es el aislamiento del enunciado (§9) y no se negocia.
 *
 * <p>Se traduce a 404 con el código estable {@code not-found}.
 */
public class ReservationProcessNotFoundException extends ApplicationException {

    public ReservationProcessNotFoundException(long processId) {
        super(ErrorCode.NOT_FOUND,
                "No existe el proceso de reserva " + processId + " para este usuario.");
    }
}
