package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La reserva está en un estado que no se puede cancelar.
 *
 * <p><strong>Solo se puede cancelar una reserva confirmada, o una ya cancelada</strong>, esta
 * última de forma idempotente. Todo lo demás —aún pendiente de teléfono, fallida— no admite
 * cancelación, y avisarlo con un código propio evita que la app interprete un "no se pudo
 * cancelar" como si hubiera salido bien.
 *
 * <p>409: hay un estado vigente que se opone a lo pedido. Reintentar no lo va a cambiar, así que
 * la app no debería probar de nuevo sino mostrar el estado actual.
 */
public class ReservationNotCancellableException extends ApplicationException {

    /**
     * @param reservationProcessId proceso cuya reserva no se pudo cancelar
     */
    public ReservationNotCancellableException(String reservationProcessId) {
        super(ErrorCode.NOT_CANCELLABLE,
                "La reserva del proceso " + reservationProcessId
                        + " no está en un estado cancelable.");
    }
}
