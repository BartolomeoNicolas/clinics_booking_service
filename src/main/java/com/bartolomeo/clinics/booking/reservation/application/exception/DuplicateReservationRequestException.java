package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * Dos procesos del mismo usuario quieren la misma clave de idempotencia.
 *
 * <p><strong>Es la base de datos avisando, no un error del cliente.</strong> El índice único
 * de {@code (usuario, clave)} es la garantía real de que un reintento no cree dos procesos;
 * este aviso es la traducción de esa garantía al dominio, para que el caso de uso pueda
 * tomar el proceso que ganó en lugar de dejar pasar el error crudo de la base.
 *
 * <p>No debería llegar al cliente: el caso de uso lo atrapa y devuelve el proceso existente. Si
 * se escapa, es un error interno y se traduce a 500 con el código {@code internal-error}.
 */
public class DuplicateReservationRequestException extends ApplicationException {

    public DuplicateReservationRequestException(long userId, String requestKey) {
        super(ErrorCode.INTERNAL_ERROR,
                "Ya existe un proceso de reserva del usuario " + userId
                        + " para la clave de idempotencia " + requestKey + ".");
    }
}
