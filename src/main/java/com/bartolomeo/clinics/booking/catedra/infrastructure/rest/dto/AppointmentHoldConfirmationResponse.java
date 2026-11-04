package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Respuesta {@code 202 Accepted} de {@code POST /api/appointment-holds/{holdId}/confirm}.
 *
 * <p><strong>Este es el cuerpo que dispara el resto del camino.</strong> Devuelve
 * {@code WAITING_FOR_PHONE} y el límite antes del cual hay que completar el teléfono; ese
 * instante es el que se persiste en el proceso local para poder mostrar la cuenta regresiva y
 * para que la expiración sea un evento que se puede anticipar.
 *
 * <p>{@code status} llega como texto a propósito: lo escribe la cátedra y lo interpreta la
 * máquina de estados del proceso (BOOK-M2-04), que es quien tiene el vocabulario de los
 * estados de un turno. Que el cliente lo traduzca ahora sería decidir en dos lugares qué
 * significa esperar el teléfono.
 *
 * @param reservationProcessId el proceso, el identificador que dura
 * @param status               estado del proceso, hoy siempre {@code WAITING_FOR_PHONE}
 * @param expiresAt            límite para completar el proceso
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentHoldConfirmationResponse(
        String reservationProcessId,
        String status,
        Instant expiresAt) {
}
