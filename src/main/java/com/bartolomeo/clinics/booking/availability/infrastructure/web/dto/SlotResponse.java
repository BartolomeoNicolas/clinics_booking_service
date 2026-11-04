package com.bartolomeo.clinics.booking.availability.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un turno disponible en la respuesta.
 *
 * <p><strong>No es el registro de dominio.</strong> El dominio tiene {@code Slot} y éste es lo
 * que sale por la puerta HTTP: si mañana el cálculo gana un dato interno, la API no cambia con
 * él. La forma, en cambio, sí es la del contrato: fecha {@code yyyy-MM-dd} y horas
 * {@code HH:mm:ss}, que es como las escribe la cátedra al crear un retén después, cuando este
 * mismo turno se va a volver a mandar.
 *
 * @param date      día del turno
 * @param startTime inicio
 * @param endTime   fin, derivado de la duración del turno
 */
public record SlotResponse(LocalDate date, LocalTime startTime, LocalTime endTime) {
}
