package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.Instant;

/**
 * El retén que la cátedra crea para un turno, tal como responde
 * {@code POST /api/appointment-holds}.
 *
 * <p><strong>El identificador que importa es el de la reserva, no el del retén.</strong> El
 * {@code holdId} sirve para confirmar en el paso siguiente y ya no se usa después; el
 * {@code reservationProcessId} es el que acompaña al proceso durante todo su resto de vida —
 * por teléfono, por eventos de Kafka y para cancelar—. Se guardan los dos porque el contrato
 * los devuelve juntos y el que sigue necesita uno de los dos en cada momento.
 *
 * <p><strong>{@code expiresAt} es la única vigencia válida.</strong> Se guarda en el proceso
 * local para saber cuándo mostrar vencimiento, pero no crea vigencia: si el proceso local dice
 * una hora y la cátedra otra, manda la de la cátedra, y guardar una copia no le agrega un
 * segundo de vida al retén.
 *
 * @param holdId                identificador del retén, solo sirve para confirmar
 * @param reservationProcessId  identificador del proceso, el que dura
 * @param status                estado devuelto por la cátedra, hoy siempre {@code HELD}
 * @param expiresAt             momento en que el retén deja de valer
 */
public record AppointmentHold(
        String holdId,
        String reservationProcessId,
        String status,
        Instant expiresAt) {
}
