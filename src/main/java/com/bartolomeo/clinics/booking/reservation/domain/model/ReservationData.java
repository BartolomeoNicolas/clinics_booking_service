package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que el usuario pidió: un profesional, un día y una hora de inicio.
 *
 * <p><strong>No viene del cuerpo del pedido tal cual.</strong> El cuerpo trae el turno que el
 * usuario tocó en la pantalla; esta forma es la que el dominio entiende, con la hora
 * completa. Y el identificador del paciente no está acá, porque no es del turno: sale del
 * usuario que lo inició y nunca de lo que manda el cliente.
 *
 * @param professionalId profesional del turno
 * @param date           día del turno
 * @param startTime      hora de inicio, {@code HH:mm:ss}
 * @param requestKey     clave de idempotencia de la solicitud; dos llamadas con la misma clave
 *                       son la misma solicitud
 */
public record ReservationData(
        long professionalId,
        LocalDate date,
        LocalTime startTime,
        String requestKey) {
}
