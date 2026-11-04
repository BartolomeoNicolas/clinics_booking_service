package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El teléfono no cumple el formato que acepta la cátedra.
 *
 * <p><strong>Se rechaza en el límite y no se publica nada.</strong> Un teléfono con letras
 * o con demasiados dígitos no es un problema de la cátedra: es un problema de quien lo
 * escribió, y se lo dice el mismo servicio que lo va a mandar, antes de mandarlo.
 *
 * <p>El mensaje dice cuál es el formato, porque el usuario lo va a tener que escribir otra
 * vez y le sirve más saber qué se espera que saber qué falló.
 *
 * <p>Se traduce a 400 con el código estable {@code validation-error}.
 */
public class InvalidPhoneNumberException extends ApplicationException {

    public InvalidPhoneNumberException(String raw) {
        super(ErrorCode.VALIDATION_ERROR,
                "El teléfono no tiene el formato que espera la cátedra: se aceptan de 7 a 15 "
                        + "dígitos, con un signo más opcional al principio. Se recibió: " + raw + ".");
    }
}
