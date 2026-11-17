package com.bartolomeo.clinics.booking.reservation.domain.ports.out;

import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import java.util.Optional;

/**
 * Persistencia de las reservas confirmadas.
 *
 * <p>Habla de reservas del dominio: no conoce entidades ni JPA.
 *
 * <p><strong>Una reserva por proceso.</strong> El proceso es el que se guarda una sola vez y
 * el que manda, así que el índice es único: dos filas para el mismo proceso serían dos
 * reservas para lo mismo, que es justo lo que hay que impedir.
 */
public interface ReservationRepository {

    /**
     * Guarda la reserva del proceso.
     *
     * @param reservation reserva a guardar
     * @return la reserva guardada, con su identificador
     */
    Reservation save(Reservation reservation);

    /**
     * La reserva de un proceso.
     *
     * @param processId proceso local
     * @return la reserva, si existe
     */
    Optional<Reservation> findByProcessId(long processId);

    /**
     * La reserva por el identificador que le dio la cátedra.
     *
     * @param catedraReservationId identificador de la reserva en la cátedra
     * @return la reserva, si existe
     */
    Optional<Reservation> findByCatedraReservationId(long catedraReservationId);
}
