package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.PageResult;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;

/**
 * Las reservas del usuario de la sesión.
 */
public interface ListMyReservationsUseCase {

    /**
     * Los procesos de reserva del usuario, paginados.
     *
     * <p><strong>El filtro por usuario no es opcional.</strong> No hay forma de pedir las
     * reservas de otro: el usuario sale del token y la consulta se arma con él. La cátedra
     * devuelve las reservas de toda la cuenta técnica y este servicio nunca las muestra
     * todas: son las del usuario que pregunta o no son nada.
     *
     * @param login usuario de la sesión
     * @param page  número de página, empezando en cero
     * @param size  tamaño de la página
     * @return la página de procesos
     */
    PageResult<ReservationProcess> list(String login, int page, int size);
}
