package com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalTime;

/**
 * Regla de agenda tal como la devuelve
 * {@code GET /api/catalog/professionals/{id}/weekly-schedules}.
 *
 * <p>Este es, de los tres, el hilo de cable que más le importa a booking: de acá salen los
 * horarios con los que se calcula qué turnos quedan libres. Por eso los tipos son explícitos
 * hasta donde el contrato lo permite.
 *
 * <p>{@code dayOfWeek} se recibe como {@link String} y no como {@code DayOfWeek}. No es una
 * relajación: el contrato lo manda como nombre de enumeración en inglés y en mayúsculas
 * —{@code "MONDAY"}—, y dejarlo crudo acá deja el único punto donde se exige ese formato en el
 * mapper, que es donde un valor inesperado puede dar un error con nombre y apellido en lugar
 * de una excepción de Jackson sin contexto.
 *
 * <p>{@code startTime} y {@code endTime} sí van como {@code LocalTime}: {@code "09:00:00"} es
 * hora local ISO y Jackson la lee tal cual, sin texto intermedio que después haya que
 * interpretar.
 *
 * <p>Las marcas de tiempo {@code createdAt} y {@code updatedAt} no están: son metadatos de la
 * réplica del catálogo y este servicio no decide nada con ellas.
 *
 * @param id                 identificador de la regla
 * @param professionalId     profesional al que pertenece
 * @param dayOfWeek          día, como texto en mayúsculas
 * @param startTime          hora de inicio
 * @param endTime            hora de fin
 * @param slotDurationMinutes duración de cada turno
 * @param enabled            si la regla está habilitada
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogWeeklyScheduleResponse(
        long id,
        long professionalId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled) {
}
