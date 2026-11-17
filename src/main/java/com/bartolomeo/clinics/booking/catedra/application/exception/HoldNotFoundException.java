package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El retén que se quiso confirmar no existe o ya no es recuperable.
 *
 * <p>Mismo código que cualquier otro "no existe", porque lo que el cliente tiene que entender es
 * lo mismo en los tres casos. Clase propia porque el mensaje tiene que nombrar el objeto real:
 * no es que no exista la reserva, es que ese retén ya no se puede recuperar, y esa frase es la
 * que corresponde cuando alguien vuelve a una pestaña vieja.
 *
 * <p><strong>No confundir con {@link HoldExpiredException}.</strong> La cátedra manda 404 para
 * los dos, pero son filas distintas de la matriz y acá se separan: "no está" es definitivo y
 * "venció" tiene fecha y el usuario puede empezar de nuevo ahora mismo.
 */
public class HoldNotFoundException extends ApplicationException {

    public HoldNotFoundException(String holdId) {
        super(ErrorCode.NOT_FOUND,
                "La cátedra no conoce el retén " + holdId + " o ya no es recuperable.");
    }
}
