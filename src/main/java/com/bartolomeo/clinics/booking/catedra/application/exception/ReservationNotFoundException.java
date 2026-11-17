package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La cátedra no conoce la reserva que se quiere cancelar.
 *
 * <p>Clase propia y no una genérica, aunque comparta el código {@code not-found} con las otras
 * dos: el mensaje tiene que decir qué fue lo que no se encontró. Cuando alguien cancela desde
 * una lista y esto aparece, "no existe la reserva" es una frase con la que se puede empezar a
 * investigar; "no existe el recurso solicitado" no lo es.
 */
public class ReservationNotFoundException extends ApplicationException {

    public ReservationNotFoundException(String reservationProcessId) {
        super(ErrorCode.NOT_FOUND,
                "La cátedra no conoce la reserva del proceso " + reservationProcessId + ".");
    }
}
