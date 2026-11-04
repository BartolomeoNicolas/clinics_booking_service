package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El proceso ya había sido confirmado inicialmente.
 *
 * <p><strong>Es la respuesta que hace seguro reintentar confirmar.</strong> La confirmación
 * tiene efectos —pone a la espera del teléfono y dispara el primer evento—, así que un timeout
 * deja la duda clásica de "¿habrá pasado?". Que un segundo intento conteste esto y no una
 * segunda confirmación es justamente lo que convierte al reintento en algo sin consecuencias
 * nuevas: el cliente sabe que el proceso ya avanzó y solo tiene que seguir desde el estado que
 * le diga.
 *
 * <p>409: el conflicto es con un estado existente del mismo proceso.
 *
 * @param message el proceso en juego, nombrado por quien traduce: confirmar solo conoce el
 *                retén, así que la frase señala al retén y no a un id que no tiene
 */
public class ReservationAlreadyConfirmedException extends ApplicationException {

    public ReservationAlreadyConfirmedException(String message) {
        super(ErrorCode.ALREADY_CONFIRMED, message);
    }
}
