package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;

/**
 * Cancela una reserva del usuario de la sesión.
 */
public interface CancelReservationUseCase {

    /**
     * Cancela la reserva.
     *
     * <p><strong>Cancelar dos veces devuelve el estado actual.</strong> La segunda vez no
     * vuelve a cancelar: devuelve el proceso como está, que ya está cancelado. Es lo que
     * hace que el botón de cancelar del móvil pueda pulsarse dos veces sin romper nada.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @param reason   motivo de la cancelación; puede ser nulo
     * @return el proceso, ya cancelado
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException
     *         si el proceso no existe o no es de ese usuario
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException
     *         si el proceso ya no admite cancelarse
     */
    ReservationProcess cancel(long processId, String login, String reason);
}
