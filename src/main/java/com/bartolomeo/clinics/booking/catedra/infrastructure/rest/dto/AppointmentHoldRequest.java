package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cuerpo de {@code POST /api/appointment-holds}.
 *
 * <p>Todos los campos son obligatorios según el contrato, y el adaptador los arma con lo que ya
 * validó el caso de uso. Ninguno viene del cliente directamente: el profesional y el turno
 * salen de la búsqueda, y {@code externalPatientId} sale del mapeo local del usuario, nunca de
 * lo que manda la app —si pudiera mandarlo, cualquier usuario reservaría a nombre de otro.
 *
 * @param professionalId    profesional del turno
 * @param date              día del turno, en {@code yyyy-MM-dd}
 * @param startTime         hora de inicio, en {@code HH:mm} como muestra el contrato
 * @param externalPatientId usuario en la forma que lo conoce la cátedra, hasta 100 caracteres
 */
public record AppointmentHoldRequest(
        long professionalId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        String externalPatientId) {
}
