package com.bartolomeo.clinics.booking.catalog.domain.model;

/**
 * Una categoría de profesional.
 *
 * <p>La misma regla que {@link Professional}: registro inmutable, con los campos que
 * describen el recurso y sin los de cuándo se registró. {@code createdAt} y
 * {@code updatedAt} son metadatos de la réplica del catálogo, no la categoría.
 *
 * @param id          identificador
 * @param name        nombre, que es lo que se muestra
 * @param description descripción, puede venir en {@code null} y se respeta tal cual
 * @param enabled     si la categoría está habilitada
 */
public record ProfessionalCategory(
        long id,
        String name,
        String description,
        boolean enabled) {
}
