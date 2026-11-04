package com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * El sobre de un evento de la cátedra.
 *
 * <p><strong>Son los campos que todos los eventos comparten.</strong> Los que son
 * particulares de cada tipo —el vencimiento, el teléfono, los datos de la reserva— no están
 * acá: cada evento los trae en su propio tipo, que se lee cuando hace falta.
 *
 * <p><strong>Los campos desconocidos se ignoran.</strong> El contrato dice que dentro de una
 * versión pueden agregarse campos opcionales y que los consumidores deben ignorarlos. Un
 * campo que no se conoce no es un error: si lo fuera, cada campo nuevo que agregue la
 * cátedra rompería a todos los que ya estaban suscritos.
 *
 * @param eventId              identificador del evento; la clave de idempotencia
 * @param eventType            tipo del evento, en el vocabulario de la cátedra
 * @param reservationProcessId proceso de la cátedra al que pertenece
 * @param occurredAt           cuándo ocurrió, según la cátedra
 * @param schemaVersion        versión del contrato con la que se escribió
 * @param reservationId        identificador de la reserva en la cátedra; lo traen los
 *                             eventos de resultado final
 * @param reason               por qué, según la cátedra; lo traen los eventos que pueden
 *                             fallar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraEventEnvelope(
        String eventId,
        String eventType,
        String reservationProcessId,
        Instant occurredAt,
        Integer schemaVersion,
        Long reservationId,
        String reason) {
}
