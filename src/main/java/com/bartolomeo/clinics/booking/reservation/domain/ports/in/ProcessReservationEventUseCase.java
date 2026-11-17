package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEventEnvelope;

/**
 * Aplica un evento de la cátedra a un proceso de reserva.
 */
public interface ProcessReservationEventUseCase {

    /**
     * Aplica el evento al proceso que le corresponde.
     *
     * <p><strong>El mismo {@code eventId} dos veces no repite el efecto.</strong> La entrega
     * de Kafka es al menos una vez, así que el mismo mensaje puede llegar repetido; la
     * segunda vez no vuelve a tocar el proceso.
     *
     * @param envelope evento de la cátedra, ya traducido al idioma del dominio
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.UnknownReservationProcessException
     *         si el evento es de un proceso que este servicio no conoce
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException
     *         si el evento no aplica al estado en que está el proceso
     */
    void process(ReservationEventEnvelope envelope);
}
