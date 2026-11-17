package com.bartolomeo.clinics.booking.availability.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * El rango de la consulta: {@code GET /api/professionals/{id}/availability?from=&to=}.
 *
 * <p><strong>Los dos campos son obligatorios.</strong> Un solo extremo no define un rango, y
 * completar el que falta con "hoy" o con "todo el mes" sería este lado decidiendo qué preguntó
 * el usuario. Si la app quiere la semana entera, manda los dos días.
 *
 * <p><strong>El formato de fecha es ISO ({@code yyyy-MM-dd}) y está pedido explícitamente.</strong>
 * Spring acepta varias formas si no se le dice nada, y dos clientes distintos podrían mandar la
 * misma fecha escrita distinto. Un solo formato, y el error si no es ese, cae como error de
 * campo en la respuesta de problemas y no como un 500 de fecha ilegible.
 *
 * <p>Que {@code from} no sea posterior a {@code to} no se juzga acá: es una regla de
 * significado, no de formato, y vive en el caso de uso junto al resto del cálculo.
 *
 * @param from primer día del rango, inclusive
 * @param to   último día del rango, inclusive
 */
public record AvailabilityRequest(
        @NotNull(message = "El primer día del rango es obligatorio.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @NotNull(message = "El último día del rango es obligatorio.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to) {
}
