package com.bartolomeo.clinics.booking.reservation.domain.model;

/**
 * Los hechos que le pueden pasar a un proceso de reserva.
 *
 * <p><strong>Un evento es algo que pasó, no una orden.</strong> "El usuario mandó el
 * teléfono" es un evento; "mandá el teléfono" es una orden, y esa otra es un caso de uso
 * distinto. La diferencia importa: un evento puede llegar dos veces, puede llegar tarde y
 * puede llegar de un proceso que ya no está como lo dejamos.
 *
 * <p><strong>Los eventos no son los mensajes de Kafka.</strong> El mensaje
 * {@code AdditionalInformationRequested} es una cosa del contrato de la cátedra; el evento
 * {@code INITIAL_CONFIRMATION} es la cosa que pasó en el negocio. El adaptador de
 * mensajería traduce el uno en el otro, y el dominio no sabe qué topics existen.
 *
 * @see ReservationState
 */
public enum ReservationEvent {

    /** La cátedra tomó el turno y devolvió su retén. */
    HOLD_CREATED,

    /**
     * La cátedra confirmó el retén y creó la reserva esperando el teléfono.
     *
     * <p>Es el momento en que el proceso pasa a depender del usuario.
     */
    INITIAL_CONFIRMATION,

    /** El usuario mandó su teléfono. */
    PHONE_SUBMITTED,

    /**
     * La cátedra rechazó el teléfono.
     *
     * <p>Es reintentable: el usuario puede mandar otro mientras el proceso no venza.
     */
    PHONE_REJECTED,

    /** La reserva quedó confirmada. */
    CONFIRMED,

    /** El usuario canceló la reserva. */
    CANCELLED,

    /** El proceso venció antes de completarse. */
    EXPIRED,

    /** La cátedra dio el proceso por inválido. */
    INVALID,

    /**
     * El flujo no pudo completarse y no tiene sentido seguir.
     *
     * <p>Es el único evento que no viene de la cátedra: lo produce este servicio cuando
     * algo no deja seguir, como un turno que ya estaba tomado.
     */
    FAILED
}
