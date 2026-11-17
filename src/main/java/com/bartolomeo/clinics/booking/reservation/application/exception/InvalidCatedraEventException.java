package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El mensaje de la cátedra no se puede entender.
 *
 * <p><strong>Es un problema del mensaje, no del mundo.</strong> Un JSON que no se puede
 * parsear, un evento sin identificador, una versión del contrato que no se conoce: son
 * cosas que no se arreglan reintentando, porque el mensaje va a seguir siendo el mismo.
 *
 * <p>El mensaje queda registrado como irrecuperable y el offset se confirma, que es lo que
 * evita que un mensaje defectuoso quede trabando la partición para siempre.
 */
public class InvalidCatedraEventException extends ApplicationException {

    public InvalidCatedraEventException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
