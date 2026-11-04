package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.time.Instant;

/**
 * Un evento de la cátedra, ya traducido al idioma del dominio.
 *
 * <p><strong>El sobre lleva los datos que hacen falta para no repetir el efecto.</strong> El
 * {@code eventId} es la clave de idempotencia funcional del contrato: el mismo evento puede
 * llegar dos veces y la segunda no puede volver a hacer nada. El {@code reservationProcessId}
 * es el proceso de la cátedra, que es como se sabe a qué proceso pertenece el evento.
 *
 * <p><strong>El tipo ya viene traducido.</strong> El adaptador de mensajería sabe qué evento
 * de la cátedra es y a qué hecho del negocio corresponde; el dominio no conoce los nombres
 * de los mensajes de Kafka ni los topics.
 *
 * @param eventId              identificador del evento, la clave de idempotencia
 * @param event                el hecho de negocio que representa
 * @param reservationProcessId proceso de la cátedra al que pertenece
 * @param schemaVersion        versión del contrato con la que se escribió el evento
 * @param occurredAt           cuándo ocurrió, según la cátedra
 * @param catedraReservationId identificador de la reserva en la cátedra; solo lo traen los
 *                             eventos de resultado final
 * @param reason               por qué, según la cátedra; solo lo traen los eventos que
 *                             pueden fallar
 */
public record ReservationEventEnvelope(
        String eventId,
        ReservationEvent event,
        String reservationProcessId,
        int schemaVersion,
        Instant occurredAt,
        Long catedraReservationId,
        String reason) {
}
