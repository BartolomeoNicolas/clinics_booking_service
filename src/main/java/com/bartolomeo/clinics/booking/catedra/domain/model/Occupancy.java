package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una posición no disponible de un día.
 *
 * <p>Es la mitad de entrada del cálculo de disponibilidad: la cátedra dice qué huecos ya están
 * ocupados, y turnos arma los que quedan libres restando esto de la agenda semanal. No dice
 * turnos libres —lo contrario sería más fácil de equivocar—, dice lo que <em>no</em> se puede
 * tomar, que es una afirmación que la cátedra puede hacer sin tener que conocer la agenda.
 *
 * <p><strong>{@code expiresAt} es lo que distingue un hold vivo de uno muerto.</strong> Solo
 * tiene valor cuando el estado es {@code HELD}: un hold con un vencimiento en el pasado no
 * bloquea nada y su hueco vuelve a estar disponible. Que ese cálculo dependa de una hora
 * externa y no de nada guardado acá es exactamente el "la vigencia real la determina
 * {@code expiresAt}" del contrato.
 *
 * @param date      día del turno, fecha local
 * @param startTime hora de inicio del hueco ocupado
 * @param status    si está confirmado o solo retenido
 * @param expiresAt vencimiento del retén, solo para {@code HELD}; nulo para {@code CONFIRMED}
 */
public record Occupancy(
        LocalDate date,
        LocalTime startTime,
        OccupancyStatus status,
        Instant expiresAt) {
}
