package com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Categoría tal como la devuelve {@code GET /api/catalog/categories}.
 *
 * <p>La misma regla que {@link CatalogProfessionalResponse}: campos del hilo de cable,
 * ignorancia obligatoria de los que no están acá, y traducción al modelo de dominio hecha en
 * el mapper y no por Jackson directo.
 *
 * <p>{@code description} puede venir en {@code null} y se respeta tal cual: el catálogo tiene
 * categorías sin descripción y rellenarlas con vacío del lado de acá sería inventar datos.
 *
 * @param id          identificador
 * @param name        nombre, que es lo que se muestra
 * @param description descripción, puede ser {@code null}
 * @param enabled     si la categoría está habilitada
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProfessionalCategoryResponse(
        long id,
        String name,
        String description,
        boolean enabled) {
}
