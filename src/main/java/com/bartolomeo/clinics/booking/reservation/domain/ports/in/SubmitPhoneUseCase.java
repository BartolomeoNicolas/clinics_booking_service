package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;

/**
 * Manda el teléfono de un proceso que está esperando la información.
 */
public interface SubmitPhoneUseCase {

    /**
     * Publica el teléfono del proceso y deja el proceso esperando el resultado.
     *
     * <p><strong>Solo el dueño del proceso puede mandarlo.</strong> Un proceso de otro
     * usuario no existe para quien pregunta: se responde 404, no 403, porque confirmar que
     * existe ya es filtrar información.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @param phone    teléfono como lo escribió el usuario; se normaliza y se valida
     * @return el proceso, ya en estado de espera del resultado
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.InvalidPhoneNumberException
     *         si el teléfono no cumple el formato que acepta la cátedra
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException
     *         si el proceso no existe o no es de ese usuario
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.ReservationExpiredException
     *         si el proceso ya venció
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException
     *         si el proceso no está esperando información
     */
    ReservationProcess submitPhone(long processId, String login, String phone);
}
