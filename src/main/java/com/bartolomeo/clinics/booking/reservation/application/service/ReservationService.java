package com.bartolomeo.clinics.booking.reservation.application.service;

import com.bartolomeo.clinics.booking.reservation.domain.model.PageResult;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.CancelReservationUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.CreateReservationUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.GetReservationProcessUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ListMyReservationsUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.SubmitPhoneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fachada del contexto de reservas.
 *
 * <p>Es lo que inyecta el controller: el controller nunca toca un port {@code in} directamente.
 * Hoy delega en un solo caso de uso, y por eso parece más de lo que es; el día que el
 * contexto tenga que consultar, cancelar o listar procesos, este es el lugar donde se juntan
 * y donde se resuelven las excepciones que vienen de varios casos de uso.
 *
 * <p>No toca los puertos de salida: ni la base ni la cátedra. Habla únicamente con casos de
 * uso, que son los que saben qué se guarda y qué se le pide a la cátedra.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final CreateReservationUseCase createReservationUseCase;
    private final SubmitPhoneUseCase submitPhoneUseCase;
    private final ListMyReservationsUseCase listMyReservationsUseCase;
    private final GetReservationProcessUseCase getReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    /**
     * Inicia un proceso de reserva para el usuario de la sesión.
     *
     * @param login usuario que inicia la reserva, tomado del token
     * @param data  el turno pedido y la clave de idempotencia
     * @return el proceso, con el estado en que quedó
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.MissingIdempotencyKeyException
     *         si la solicitud no trae clave de idempotencia
     */
    public ReservationProcess create(String login, ReservationData data) {
        return createReservationUseCase.create(login, data);
    }

    /**
     * Manda el teléfono de un proceso que está esperando la información.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @param phone    teléfono como lo escribió el usuario
     * @return el proceso, ya esperando el resultado
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.InvalidPhoneNumberException
     *         si el teléfono no cumple el formato que acepta la cátedra
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException
     *         si el proceso no existe o no es de ese usuario
     */
    public ReservationProcess submitPhone(long processId, String login, String phone) {
        return submitPhoneUseCase.submitPhone(processId, login, phone);
    }

    /**
     * Las reservas del usuario de la sesión, paginadas.
     *
     * @param login usuario de la sesión
     * @param page  número de página, empezando en cero
     * @param size  tamaño de la página
     * @return la página de procesos
     */
    public PageResult<ReservationProcess> listMyReservations(String login, int page, int size) {
        return listMyReservationsUseCase.list(login, page, size);
    }

    /**
     * El estado de una reserva del usuario de la sesión.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @return el proceso
     */
    public ReservationProcess getReservation(long processId, String login) {
        return getReservationUseCase.get(processId, login);
    }

    /**
     * Cancela una reserva del usuario de la sesión.
     *
     * @param processId identificador local del proceso
     * @param login    usuario de la sesión
     * @param reason   motivo de la cancelación; puede ser nulo
     * @return el proceso, ya cancelado
     */
    public ReservationProcess cancelReservation(long processId, String login, String reason) {
        return cancelReservationUseCase.cancel(processId, login, reason);
    }
}
