package com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto;

import java.util.List;

/**
 * Una página de las reservas del usuario.
 *
 * <p><strong>El total y si hay más van en la respuesta.</strong> Sin ellos el cliente no sabe
 * si hay más y termina pidiendo páginas vacías una detrás de otra.
 *
 * @param content las reservas de la página
 * @param page    número de página, empezando en cero
 * @param size    tamaño de la página
 * @param total   cuántas reservas hay en total
 * @param hasMore si hay más páginas después de esta
 */
public record ReservationPageResponse(
        List<ReservationProcessResponse> content,
        int page,
        int size,
        long total,
        boolean hasMore) {
}
