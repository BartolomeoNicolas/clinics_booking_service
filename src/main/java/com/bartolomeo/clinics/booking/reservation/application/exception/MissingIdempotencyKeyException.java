package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La solicitud no trae clave de idempotencia.
 *
 * <p><strong>Es un error del pedido y no un detalle opcional.</strong> Sin una clave que
 * identifique la solicitud no hay forma de saber si un reintento es el mismo intento o una
 * reserva nueva, y el peor resultado posible —crear dos reservas para lo mismo— nace
 * justamente de adivinar. Pedir la clave es más honesto que deducirla de los datos del turno,
 * porque un usuario puede querer dos reservas para el mismo profesional y el mismo día, en
 * horarios distintos, y eso solo se distingue por la clave.
 *
 * <p>Se traduce a 400 con el código estable {@code validation-error}.
 */
public class MissingIdempotencyKeyException extends ApplicationException {

    public MissingIdempotencyKeyException() {
        super(ErrorCode.VALIDATION_ERROR,
                "La solicitud de reserva necesita una clave de idempotencia.");
    }
}
