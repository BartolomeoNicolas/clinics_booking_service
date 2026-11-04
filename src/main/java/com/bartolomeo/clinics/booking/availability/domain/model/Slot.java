package com.bartolomeo.clinics.booking.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un turno ofertable: un hueco de la agenda de un profesional en un momento concreto.
 *
 * <p><strong>Es lo que el usuario elige, no lo que la cátedra tiene.</strong> Un slot nace de
 * cruzar la regla semanal vigente del catálogo con las ocupaciones del día (enunciado §7): la
 * regla dice de qué a qué hora se atiende, las ocupaciones dicen qué huecos ya están tomados, y
 * lo que sobra es esto. Que exista un slot no garantiza que se pueda reservar —eso lo decide
 * la cátedra al crear el retén—, solo garantiza que hasta ahora nadie lo tomó.
 *
 * <p><strong>El {@code endTime} sale de la duración del turno y no de un fin de la
 * regla.</strong> La agenda dice {@code 09:00 a 13:00}; de ahí salen turnos de media hora, y
 * cada turno termina cuando empieza el siguiente. Se incluye en la respuesta porque es el
 * dato que la pantalla necesita para mostrar "09:00–09:30" sin que la app tenga que volver a
 * calcular la duración.
 *
 * <p><strong>Las horas van completas ({@code HH:mm:ss}).</strong> Es la forma en que las
 * escribe el catálogo y en que las pide la cátedra al crear un retén; no hay conversión en el
 * medio, porque cada conversión es una oportunidad de que dos lados escriban la misma hora
 * distinto.
 *
 * @param date      día del turno
 * @param startTime inicio, dentro de la ventana de la regla
 * @param endTime   inicio más la duración del turno; siempre {@code > startTime} y nunca
 *                  pasa de medianoche
 */
public record Slot(LocalDate date, LocalTime startTime, LocalTime endTime) {
}
