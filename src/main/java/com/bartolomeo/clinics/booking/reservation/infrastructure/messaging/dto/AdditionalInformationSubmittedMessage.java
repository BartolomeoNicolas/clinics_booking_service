package com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.dto;

import java.time.Instant;

/**
 * Cuerpo de {@code AdditionalInformationSubmitted}.
 *
 * <p><strong>Todos los campos son obligatorios</strong> y el contrato no perdona: si falta
 * uno, la cátedra rechaza el mensaje y el usuario no sabe que el teléfono no llegó. Por eso
 * el mensaje se arma entero desde el proceso y no se arma a medias.
 *
 * @param eventId              identificador de este mensaje
 * @param eventType            siempre {@code AdditionalInformationSubmitted}
 * @param groupId              grupo de la cuenta técnica
 * @param reservationProcessId proceso de la cátedra; también es la clave del mensaje
 * @param holdId               retén sobre el que se manda la información
 * @param requestEventId       evento que pidió la información
 * @param information          la información pedida
 * @param occurredAt           cuándo se mandó
 * @param schemaVersion        versión del contrato
 */
public record AdditionalInformationSubmittedMessage(
        String eventId,
        String eventType,
        String groupId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Information information,
        Instant occurredAt,
        int schemaVersion) {

    /** La información que la cátedra pidió. */
    public record Information(String phoneNumber) {
    }
}
