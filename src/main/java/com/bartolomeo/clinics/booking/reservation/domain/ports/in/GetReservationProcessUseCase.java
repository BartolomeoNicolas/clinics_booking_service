package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;

/**
 * El estado de una reserva del usuario de la sesión.
 */
public interface GetReservationProcessUseCase {

    /**
     * El proceso de reserva del usuario.
     *
     * <p><strong>Un proceso de otro usuario no existe.</strong> Se responde 404 y no 403:
     * confirmar que el proceso existe pero es de otro ya es filtrar información.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @return el proceso
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException
     *         si el proceso no existe o no es de ese usuario
     */
    ReservationProcess get(long processId, String login);
}
