package com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * El turno que el usuario eligió: {@code POST /api/reservations}.
 *
 * <p><strong>No incluye el usuario.</strong> El usuario sale del token: si el cuerpo pudiera
 * traerlo, cualquiera podría reservar a nombre de otro. Tampoco incluye la clave de
 * idempotencia, que va en el header y es cosa del transporte, no del turno.
 *
 * <p><strong>Los formatos están declarados.</strong> La fecha es {@code yyyy-MM-dd} y la hora
 * {@code HH:mm:ss}, que es como este servicio escribe las horas en todas sus respuestas: la
 * app no tiene que adivinar qué formato espera cada lado.
 *
 * @param professionalId profesional del turno
 * @param date           día del turno
 * @param startTime      hora de inicio del turno
 */
public record CreateReservationRequest(
        @NotNull(message = "El profesional es obligatorio.")
        @Positive(message = "El profesional del turno no es válido.")
        Long professionalId,

        @NotNull(message = "La fecha del turno es obligatoria.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,

        @NotNull(message = "La hora del turno es obligatoria.")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime startTime) {
}
