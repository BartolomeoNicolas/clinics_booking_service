package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Respuesta {@code 201 Created} de {@code POST /api/appointment-holds}.
 *
 * @param holdId               identificador del retén, solo sirve para confirmar
 * @param reservationProcessId identificador del proceso, el que dura
 * @param status               estado devuelto, hoy siempre {@code HELD}
 * @param expiresAt            vigencia real del retén, que es la única que cuenta
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentHoldResponse(
        String holdId,
        String reservationProcessId,
        String status,
        Instant expiresAt) {
}
