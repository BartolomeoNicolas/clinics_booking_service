package com.bartolomeo.clinics.booking.reservation.domain.ports.out;

/**
 * Los eventos que ya se aplicaron.
 *
 * <p><strong>Es la idempotencia, guardada.</strong> Kafka entrega al menos una vez, así que el
 * mismo mensaje puede llegar dos veces y la segunda tiene que ser inocua. La clave es el
 * {@code eventId}, que el contrato define como la clave de idempotencia funcional.
 *
 * <p>Habla de eventos ya procesados: no conoce entidades ni JPA.
 */
public interface ProcessedEventRepository {

    /**
     * Si el evento ya se aplicó alguna vez.
     *
     * @param eventId identificador del evento
     * @return {@code true} si ya está en la lista
     */
    boolean existsByEventId(String eventId);

    /**
     * Anota el evento como aplicado.
     *
     * @param eventId       identificador del evento
     * @param schemaVersion versión del contrato con la que se escribió
     */
    void save(String eventId, int schemaVersion);
}
