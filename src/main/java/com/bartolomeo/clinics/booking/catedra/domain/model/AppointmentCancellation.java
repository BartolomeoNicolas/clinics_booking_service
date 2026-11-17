package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.Instant;

/**
 * Lo que contesta la cátedra al cancelar, tal como responde
 * {@code POST /api/appointments/{reservationProcessId}/cancel}.
 *
 * <p><strong>La cancelación es idempotente, y eso se nota en este modelo:</strong> si la reserva
 * ya estaba cancelada, la cátedra devuelve su estado actual en vez de un error, y este registro
 * es idéntico al de la primera vez. Por eso el resultado de cancelar dos veces es el mismo
 * objeto y no una segunda operación con consecuencias.
 *
 * @param reservationId         id de la reserva en la cátedra
 * @param reservationProcessId  id del proceso asociado
 * @param status                estado final, siempre {@code CANCELLED}
 * @param cancelledAt           cuándo se canceló
 */
public record AppointmentCancellation(
        long reservationId,
        String reservationProcessId,
        String status,
        Instant cancelledAt) {
}
