package com.bartolomeo.clinics.booking.reservation.domain.model;

/**
 * La respuesta con el teléfono, lista para publicar.
 *
 * <p><strong>El {@code eventId} es nuevo en cada envío.</strong> Si se reusa, la cátedra
 * vería dos veces el mismo mensaje y no podría distinguirlos: un reenvío después de un
 * rechazo es un mensaje nuevo, con su propia identidad.
 *
 * <p><strong>El {@code requestEventId} es el del pedido vigente.</strong> Es el evento que
 * pidió el teléfono, y es lo que le dice a la cátedra a qué pedido responde esto.
 *
 * @param eventId              identificador de este mensaje
 * @param reservationProcessId proceso de la cátedra; también es la clave del mensaje
 * @param holdId               retén sobre el que se pide el teléfono
 * @param requestEventId       evento que pidió la información
 * @param phoneNumber          teléfono ya normalizado
 */
public record PhoneSubmission(
        String eventId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        String phoneNumber) {
}
