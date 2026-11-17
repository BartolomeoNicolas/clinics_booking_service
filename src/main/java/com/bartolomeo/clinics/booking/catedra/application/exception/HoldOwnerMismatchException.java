package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El retén pertenece a otra integración técnica.
 *
 * <p>Este servicio usa una sola cuenta técnica, así que en el camino feliz no puede ocurrir:
 * todos los retenes del proyecto son suyos. Se traduce igual, y por eso va documentado, porque
 * <strong>que una condición sea improbable no es una justificación para no traducirla</strong>.
 * Si algún día cambia la cuenta, el error aparecería sin fila en la matriz y terminaría en un
 * 500 genérico que no dice nada —mientras que acá queda en 403 con un nombre que se puede leer
 * en el log.
 *
 * <p>403, como en la cátedra: no es que el retén no exista, es que no nos pertenece.
 */
public class HoldOwnerMismatchException extends ApplicationException {

    public HoldOwnerMismatchException(String holdId) {
        super(ErrorCode.FORBIDDEN,
                "El retén " + holdId + " pertenece a otra integración técnica.");
    }
}
