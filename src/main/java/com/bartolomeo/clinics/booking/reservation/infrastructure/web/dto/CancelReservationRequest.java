package com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto;

/**
 * El motivo de una cancelación.
 *
 * <p><strong>Es opcional.</strong> El contrato permite cancelar sin motivo, y la cátedra lo
 * acepta nulo.
 *
 * @param reason por qué cancela el usuario
 */
public record CancelReservationRequest(String reason) {
}
