package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El retén venció antes de que se pudiera confirmar.
 *
 * <p><strong>Es la excepción con más carga semántica de esta matriz</strong>, y por eso tiene
 * código propio aunque la cátedra lo mande con el mismo 404 que un retén inexistente. Un hold
 * vencido significa una sola cosa: el turno volvió a estar libre. Quien la recibe tiene que
 * cerrar el proceso local, mostrar el vencimiento y ofrecer empezar de nuevo —exactamente lo
 * que pide el anexo 18.3— y eso no se puede decidir con un {@code not-found} genérico, que
 * también significa que el id estaba mal.
 *
 * <p>Se conserva el 404 de la cátedra: para este servicio el retén, como identificador útil,
 * efectivamente dejó de existir.
 */
public class HoldExpiredException extends ApplicationException {

    public HoldExpiredException(String holdId) {
        super(ErrorCode.HOLD_EXPIRED,
                "El retén " + holdId + " venció y el turno quedó libre.");
    }
}
