package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;

/**
 * Inicia un proceso de reserva.
 */
public interface CreateReservationUseCase {

    /**
     * Toma el turno pedido: pide el retén a la cátedra y lo confirma, dejando el proceso
     * esperando el teléfono.
     *
     * <p><strong>Repetir la misma solicitud no crea un segundo proceso.</strong> La clave de
     * idempotencia que viene en la solicitud identifica la operación: si el proceso ya existe,
     * se sigue desde donde está en vez de arrancar de cero. Eso es lo que hace que un reintento
     * del cliente —después de un timeout, con la respuesta perdida— no deje dos reservas.
     *
     * @param login login del usuario que inicia la reserva; sale del token, nunca del cuerpo
     * @param data  el turno pedido y la clave de idempotencia
     * @return el proceso, con el estado en que quedó
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.MissingIdempotencyKeyException
     *         si la solicitud no trae clave de idempotencia
     * @throws com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException
     *         si el login no corresponde a un usuario
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     *         si la cátedra no está disponible y el estado no se puede reconciliar
     */
    ReservationProcess create(String login, ReservationData data);
}
