package com.bartolomeo.clinics.booking.catedra.domain.model;

/**
 * Estados en los que la cátedra puede tener una reserva.
 *
 * <p>Son los cuatro que acepta el filtro de {@code GET /api/appointments} y los cuatro que
 * aparecen en el contrato. Existe como enumeración —y no como texto— porque es un conjunto
 * cerrado del otro lado: si la cátedra agrega un estado, esto tiene que romperse en el lugar
 * donde se lee el contrato y no convertirse en una cadena que ningún filtro distingue.
 *
 * <p><strong>No confundir con la máquina de estados del proceso local</strong> (BOOK-M2-04).
 * La reserva de la cátedra y el proceso de turnos tienen vidas distintas y vocabularios
 * distintos: allá hay cuatro estados, acá hay diez. Mapear uno a otro es trabajo de la máquina
 * de estados, no del cliente.
 */
public enum CatedraReservationStatus {

    /** Reserva creada, esperando el teléfono. */
    PHONE_PENDING,

    /** Reserva completa. */
    CONFIRMED,

    /** Reserva cancelada. */
    CANCELLED,

    /** Reserva que no se pudo completar. */
    FAILED
}
