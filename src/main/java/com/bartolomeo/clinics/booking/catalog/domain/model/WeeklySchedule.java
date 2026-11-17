package com.bartolomeo.clinics.booking.catalog.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Una regla semanal de la agenda de un profesional.
 *
 * <p><strong>Es la pieza con la que se calcula la disponibilidad</strong> (enunciado §7): el
 * servicio de turnos combina estos horarios con las ocupaciones de la cátedra para armar los
 * slots que ve el usuario. Por eso el modelo está acá y no en {@code availability}: la regla
 * pertenece al catálogo, y la decisión de qué slots quedan libres es de turnos.
 *
 * <p><strong>Se traen también las reglas apagadas.</strong> El catálogo las devuelve así, y
 * descartarlas del lado del cliente escondería el caso que turnos tiene que rechazar: una
 * agenda deshabilitada no genera slots, y esa regla tiene que poder probarse.
 *
 * <p>{@code dayOfWeek} es una enumeración y no un texto. El contrato lo manda en inglés y en
 * mayúsculas —{@code "MONDAY"}—, y traducirlo acá de una vez es lo que permite que ningún caso
 * de uso tenga que comparar cadenas ni preguntarse si el texto viene bien escrito.
 *
 * @param id                 identificador de la regla en el catálogo
 * @param professionalId     profesional al que pertenece
 * @param dayOfWeek          día de la semana
 * @param startTime          hora de inicio, {@code HH:mm:ss}
 * @param endTime            hora de fin, {@code HH:mm:ss}
 * @param slotDurationMinutes duración de cada turno que se deriva de esta regla
 * @param enabled            si la regla está habilitada
 */
public record WeeklySchedule(
        long id,
        long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled) {
}
