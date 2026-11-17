package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El horario pedido no cae dentro de la agenda habilitada del profesional.
 *
 * <p><strong>Es distinto de "ya está ocupado".</strong> Un slot ocupado estaba bien pedido y
 * alguien se le adelantó; este nunca existió como opción —la hora es fuera del horario de
 * atención, el día no tiene agenda o la regla está apagada—. La reacción de la app es
 * distinta: en un caso pide elegir otro hueco, en este caso pide elegir otro momento.
 *
 * <p>Cuando aparece con un profesional y una fecha que recién salieron del catálogo, casi
 * siempre es una carrera: la agenda cambió entre la consulta y la reserva. Eso es un caso
 * normal, no un bug, y el código propio permite que lo sea.
 */
public class InvalidSlotException extends ApplicationException {

    public InvalidSlotException(String message) {
        super(ErrorCode.INVALID_SLOT, message);
    }
}
