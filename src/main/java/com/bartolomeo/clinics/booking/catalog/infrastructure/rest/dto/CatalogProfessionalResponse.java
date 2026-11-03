package com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ficha de profesional tal como la devuelve {@code GET /api/catalog/professionals/{id}}.
 *
 * <p><strong>Modela el hilo de cable y no el dominio.</strong> Este registro existe para que la
 * única dependencia que tiene booking del servicio de catálogo sea visible en un solo archivo:
 * qué campos trae, con qué tipo y cómo se llama cada uno. Si mañana el catálogo cambia de
 * forma, se cambia acá y en el mapper, y el dominio no se entera.
 *
 * <p><strong>{@code @JsonIgnoreProperties} no es decoración, es requisito.</strong> La
 * respuesta real trae también {@code createdAt}, {@code updatedAt} y un arreglo
 * {@code weeklySchedules} completo, y este servicio no modela ninguno de los tres. Con la
 * anotación —y con que Jackson esté configurado para pasar de largo lo que no conoce— esos
 * campos se ignoran en vez de reventar la lectura. Es exactamente lo que el test de contrato
 * comprueba contra la respuesta capturada del servicio real: un campo nuevo del otro lado no
 * puede romper a este lado.
 *
 * @param id         identificador
 * @param categoryId categoría a la que pertenece
 * @param firstName  nombre
 * @param lastName   apellido
 * @param enabled    si está habilitado
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProfessionalResponse(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled) {
}
