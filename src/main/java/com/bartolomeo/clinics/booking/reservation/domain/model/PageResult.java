package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.util.List;

/**
 * Una página de resultados.
 *
 * <p><strong>El total va aparte.</strong> Con el total se puede responder "mostrar más" sin
 * tener que traer la página entera; sin él, el cliente no sabe si hay más y termina pidiendo
 * páginas vacías.
 *
 * @param content los elementos de la página
 * @param page    número de página, empezando en cero
 * @param size    tamaño pedido
 * @param total   cuántos elementos hay en total
 * @param <T>     el tipo de los elementos
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long total) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /** Si hay más páginas después de esta. */
    public boolean hasMore() {
        return (long) (page + 1) * size < total;
    }
}
