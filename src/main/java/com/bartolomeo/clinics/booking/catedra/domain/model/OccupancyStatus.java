package com.bartolomeo.clinics.booking.catedra.domain.model;

/**
 * Por qué una posición está ocupada, según la cátedra.
 *
 * <p>Son solo dos valores porque el contrato tiene solo dos, y porque las dos razones se tratan
 * distinto al calcular: un {@code CONFIRMED} no se puede ofrecer nunca, y un {@code HELD} se
 * puede volver a ofrecer en cuanto su {@code expiresAt} pasa.
 *
 * <p><strong>Si un hueco aparece en los dos orígenes, prevalece {@code CONFIRMED}</strong>, que
 * es lo que dice el contrato. Esa resolución ocurre del lado de turnos y no de la cátedra, y
 * por eso el enum existe acá y no solo allá.
 */
public enum OccupancyStatus {

    /** Reserva confirmada. No vuelve a estar disponible. */
    CONFIRMED,

    /** Retén activo de otra reserva en curso. Se libera al vencer su {@code expiresAt}. */
    HELD
}
