package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El mensaje con el teléfono no se pudo publicar.
 *
 * <p><strong>Es un problema de infraestructura y no del pedido.</strong> El teléfono estaba
 * bien escrito y el proceso estaba en condiciones de mandarlo: lo que falló fue el broker.
 *
 * <p>Se traduce a 503 con el código {@code catedra-unavailable}, que es lo que le dice al
 * cliente que puede reintentar. Y como el proceso no se guarda como "teléfono enviado"
 * cuando el mensaje no salió, el reintento vuelve a intentarlo de verdad.
 */
public class PhoneSubmissionFailedException extends ApplicationException {

    public PhoneSubmissionFailedException(String message, Throwable cause) {
        super(ErrorCode.CATEDRA_UNAVAILABLE, message, cause);
    }
}
