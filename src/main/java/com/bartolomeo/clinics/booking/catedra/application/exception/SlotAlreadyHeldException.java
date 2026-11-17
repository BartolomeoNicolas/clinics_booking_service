package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * Otro proceso tiene un retén activo sobre el mismo turno.
 *
 * <p>Mismo código que {@link SlotAlreadyReservedException} y misma razón: para quien reserva,
 * la consecuencia es idéntica y elegir otro turno es la única salida. Lo que cambia —y lo que
 * hace que valga la pena una clase distinta— es que un retén <em>sí</em> se libera solo, cuando
 * su {@code expiresAt} pasa, mientras que una confirmación no. Tenerlo nombrado deja que quien
 * lea el log distinga un conflicto transitorio de uno permanente sin parsear un mensaje.
 *
 * @param message el turno en juego, nombrado por quien traduce
 */
public class SlotAlreadyHeldException extends ApplicationException {

    public SlotAlreadyHeldException(String message) {
        super(ErrorCode.SLOT_NOT_AVAILABLE, message);
    }
}
