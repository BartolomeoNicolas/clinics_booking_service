package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Respuesta {@code 200 OK} de {@code POST /api/appointments/{reservationProcessId}/cancel}.
 *
 * <p><strong>Cancelar dos veces devuelve exactamente este mismo cuerpo</strong>, porque la
 * cancelación es idempotente: si la reserva ya estaba cancelada, la cátedra informa su estado
 * actual en vez de fallar. Eso es lo que hace seguro que un timeout en la cancelación se
 * resuelva simplemente llamando de nuevo —el resultado es el mismo y no hay un segundo efecto.
 *
 * @param reservationId         id de la reserva en la cátedra
 * @param reservationProcessId  id del proceso asociado
 * @param status                estado final, siempre {@code CANCELLED}
 * @param cancelledAt           cuándo se canceló
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCancellationResponse(
        long reservationId,
        String reservationProcessId,
        String status,
        Instant cancelledAt) {
}
