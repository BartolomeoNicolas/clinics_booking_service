package com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * El proceso de reserva, como lo ve la app.
 *
 * <p><strong>El identificador del proceso es el local.</strong> Es el que existe antes de que
 * la cátedra haya contestado, y por eso es el que la app puede usar para consultar el estado
 * aunque la reserva todavía no haya arrancado. El {@code reservationProcessId} de la cátedra
 * viene aparte, porque no siempre lo hay y porque no es el que la app usa para hablar con
 * ella.
 *
 * <p><strong>El retén no sale.</strong> Es un detalle de cómo se llegó a la reserva y no algo
 * que la app tenga que manejar.
 *
 * @param processId           identificador local del proceso
 * @param state               estado en que quedó
 * @param professionalId      profesional del turno
 * @param date                día del turno
 * @param startTime           hora de inicio del turno
 * @param reservationProcessId proceso en la cátedra; puede no haber todavía
 * @param expiresAt           vencimiento, el de la cátedra
 */
public record ReservationProcessResponse(
        long processId,
        String state,
        long professionalId,
        LocalDate date,
        LocalTime startTime,
        String reservationProcessId,
        Instant expiresAt) {
}
