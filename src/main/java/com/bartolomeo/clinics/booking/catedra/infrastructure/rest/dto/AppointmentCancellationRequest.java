package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

/**
 * Cuerpo de {@code POST /api/appointments/{reservationProcessId}/cancel}.
 *
 * <p><strong>El motivo es opcional, y por eso este registro se manda igual que venga.</strong>
 * Un {@code null} se serializa como {@code {"reason": null}}, que el DTO del otro lado acepta
 * sin problema; mandar un cuerpo vacío obligaría a dos ramas distintas para una diferencia que
 * no cambia nada. Lo que sí hay que tener presente es que la longitud es del contrato: hasta
 * 500 caracteres, y si se pasa, la cátedra contesta {@code VALIDATION_ERROR}, que este servicio
 * traduce a una validación propia.
 *
 * @param reason motivo de la cancelación, hasta 500 caracteres; puede ser nulo
 */
public record AppointmentCancellationRequest(String reason) {
}
