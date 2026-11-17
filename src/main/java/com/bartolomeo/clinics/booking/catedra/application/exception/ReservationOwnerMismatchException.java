package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La reserva pertenece a otra integración técnica.
 *
 * <p>Misma razón que {@link HoldOwnerMismatchException} para estar aunque hoy no pueda pasar:
 * este servicio habla con una sola cuenta, así que todas las reservas del listado son suyas.
 * Se traduce para que, si algún día cambia eso, el error tenga nombre en lugar de caer en el
 * 500 genérico.
 *
 * <p>Este es además el caso que protege el aislamiento entre usuarios: la cátedra devuelve las
 * reservas de <em>toda</em> la cuenta técnica, y que ella marque las ajenas es la primera
 * línea; la segunda es que booking filtre por su propio usuario, que es donde realmente vive la
 * separación (BOOK-M2-09).
 */
public class ReservationOwnerMismatchException extends ApplicationException {

    public ReservationOwnerMismatchException(String reservationProcessId) {
        super(ErrorCode.FORBIDDEN,
                "La reserva del proceso " + reservationProcessId
                        + " pertenece a otra integración técnica.");
    }
}
