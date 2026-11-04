package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Respuesta de {@code GET /api/appointment-occupancies}.
 *
 * <p>El eco de {@code professionalId}, {@code from} y {@code to} se recibe y se ignora: es lo
 * que mandamos devuelto, no aporta nada y confiar en que coincida sería darle a la cátedra la
 * responsabilidad de decirnos qué preguntamos. Lo que importa es la lista.
 *
 * @param professionalId eco del filtro, se descarta
 * @param from           eco del filtro, se descarta
 * @param to             eco del filtro, se descarta
 * @param occupancies    posiciones no disponibles del rango; puede venir vacía
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OccupanciesResponse(
        Long professionalId,
        LocalDate from,
        LocalDate to,
        List<OccupancyItem> occupancies) {

    /**
     * Una posición no disponible, en la forma en que la escribe la cátedra.
     *
     * <p>{@code startTime} se recibe como texto de hora —acepta {@code HH:mm} y
     * {@code HH:mm:ss}, que es lo que dice el contrato— y se convierte a {@link LocalTime}.
     *
     * @param date      día del turno
     * @param startTime hora de inicio
     * @param status    {@code CONFIRMED} o {@code HELD}
     * @param expiresAt vencimiento del retén; nulo cuando el estado es {@code CONFIRMED}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OccupancyItem(
            LocalDate date,
            LocalTime startTime,
            OccupancyStatus status,
            Instant expiresAt) {
    }
}
