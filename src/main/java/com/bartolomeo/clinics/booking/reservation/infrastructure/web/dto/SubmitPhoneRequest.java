package com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El teléfono que el usuario manda: {@code POST /api/reservations/{processId}/phone}.
 *
 * <p><strong>Se manda como lo escribió el usuario.</strong> La normalización —quitar
 * espacios, guiones, paréntesis y puntos— es la de la cátedra y la hace el dominio, no el
 * transporte: si se normalizara acá, el servicio dejaría de aceptar teléfono por lo que la
 * cátedra no acepta.
 *
 * @param phoneNumber teléfono del usuario
 */
public record SubmitPhoneRequest(
        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 32, message = "El teléfono es demasiado largo.")
        String phoneNumber) {
}
