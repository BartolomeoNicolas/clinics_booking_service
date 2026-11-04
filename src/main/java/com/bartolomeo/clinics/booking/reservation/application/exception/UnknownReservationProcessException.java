package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El evento es de un proceso que este servicio no conoce.
 *
 * <p><strong>No es un error de la cátedra ni del cliente.</strong> Es un evento que habla de
 * un proceso que no está en la base local: puede ser de otra cuenta técnica, de un proceso
 * que se creó antes de que este servicio existiera, o de uno que ya se borró. Lo que no se
 * puede es aplicarlo, porque no hay a qué proceso aplicarlo.
 *
 * <p>El mensaje queda registrado como irrecuperable y el offset se confirma: reintentarlo no
 * va a hacer que el proceso aparezca solo, y dejarlo sin confirmar haría que el mismo
 * mensaje volviera a llegar para siempre.
 */
public class UnknownReservationProcessException extends ApplicationException {

    public UnknownReservationProcessException(String reservationProcessId) {
        super(ErrorCode.INTERNAL_ERROR,
                "El evento es del proceso " + reservationProcessId
                        + ", que este servicio no conoce.");
    }
}
