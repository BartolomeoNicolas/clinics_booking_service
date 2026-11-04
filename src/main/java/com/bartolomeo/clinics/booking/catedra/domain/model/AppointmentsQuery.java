package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.LocalDate;

/**
 * Criterios de búsqueda de reservas contra la cátedra.
 *
 * <p>Todos opcionales y combinables, como manda el contrato, porque la cátedra los recibe como
 * query params opcionales. Los que quedan en {@code null} simplemente no se mandan: agregar un
 * parámetro con valor vacío es una forma segura de que el otro lado filtre por vacío en lugar
 * de no filtrar.
 *
 * <p><strong>Este criterio no es un criterio de "mis reservas".</strong> La cátedra devuelve
 * las de toda la cuenta técnica; el filtro por usuario se aplica aparte y es responsabilidad de
 * booking. Servir esta lista sin filtrar es exactamente el escape de aislamiento que el
 * enunciado prohíbe.
 *
 * @param externalPatientId usuario a buscar, coincidencia exacta
 * @param status            estado a filtrar
 * @param from              fecha mínima inclusiva
 * @param to                fecha máxima inclusiva
 * @param page              página, desde 0
 * @param size              tamaño de página
 */
public record AppointmentsQuery(
        String externalPatientId,
        CatedraReservationStatus status,
        LocalDate from,
        LocalDate to,
        int page,
        int size) {

    /** Criterio sin filtros, primera página con el tamaño por defecto de la cátedra. */
    public static AppointmentsQuery all() {
        return new AppointmentsQuery(null, null, null, null, 0, 20);
    }
}
