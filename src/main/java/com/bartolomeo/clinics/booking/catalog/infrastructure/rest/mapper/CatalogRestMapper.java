package com.bartolomeo.clinics.booking.catalog.infrastructure.rest.mapper;

import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.ProfessionalCategory;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogProfessionalCategoryResponse;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogProfessionalResponse;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogWeeklyScheduleResponse;
import java.time.DayOfWeek;

/**
 * El traductor entre lo que manda el catálogo y lo que entiende este servicio.
 *
 * <p>Es el lugar exacto donde se paga la decisión de tener dos formas de cada objeto. El DTO
 * vive para reflejar el hilo de cable y nada más; este mapper vive para que el dominio nunca
 * tenga que saber cómo se escribe un día de la semana en el otro servicio.
 *
 * <p>En total son tres filas de código por recurso, y vale la pena el archivo: sin él, o bien
 * el dominio usaría los tipos del cable —{@code String} para el día, un objeto anónimo para
 * lo que llega de más—, o bien habría que poner anotaciones de Jackson en los modelos de
 * negocio, que es justo lo que la arquitectura hexagonal busca no tener.
 *
 * <p>Las traducciones que pueden fallar fallan acá y no en silencio:
 * {@link DayOfWeek#valueOf} lanza si el catálogo manda un día que no existe, y esa excepción
 * no se traga. Preferimos un error tajante ante un contrato violado que una agenda que aparece
 * vacía porque no supimos leerla.
 */
public final class CatalogRestMapper {

    private CatalogRestMapper() {
    }

    public static Professional toProfessional(CatalogProfessionalResponse response) {
        return new Professional(
                response.id(),
                response.categoryId(),
                response.firstName(),
                response.lastName(),
                response.enabled());
    }

    public static ProfessionalCategory toProfessionalCategory(
            CatalogProfessionalCategoryResponse response) {
        return new ProfessionalCategory(
                response.id(),
                response.name(),
                response.description(),
                response.enabled());
    }

    public static WeeklySchedule toWeeklySchedule(CatalogWeeklyScheduleResponse response) {
        if (response.dayOfWeek() == null) {
            throw new IllegalArgumentException(
                    "El catálogo mando una regla de agenda sin dia de la semana: id="
                            + response.id());
        }
        return new WeeklySchedule(
                response.id(),
                response.professionalId(),
                DayOfWeek.valueOf(response.dayOfWeek()),
                response.startTime(),
                response.endTime(),
                response.slotDurationMinutes(),
                response.enabled());
    }
}
