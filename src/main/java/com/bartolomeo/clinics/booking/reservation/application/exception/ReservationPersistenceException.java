package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La escritura del proceso no se pudo hacer.
 *
 * <p><strong>Es un error de este lado y no del cliente.</strong> La base no aceptó la fila
 * por una restricción que no es la de idempotencia, y no hay forma de saber cuál fue sin
 * mirar la base: lo que se conserva es la causa original en el log, que es donde sirve.
 *
 * <p>Se traduce a 500 con el código {@code internal-error}. Que un error de persistencia sea
 * un 500 y no un 400 es lo que le dice al cliente que no tiene que cambiar nada para
 * resolverlo.
 */
public class ReservationPersistenceException extends ApplicationException {

    public ReservationPersistenceException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
