package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un elemento del arreglo que devuelve {@code GET /api/appointments}.
 *
 * <p><strong>Los nombres del profesional y del paciente son una fotografía, no el
 * catálogo.</strong> Son el nombre que tenían en el momento de la reserva, y por eso están
 * acá: consultar cómo se llama el profesional <em>hoy</em> es siempre una pregunta para el
 * catálogo, y confundir las dos fuentes es exactamente el escape de contrato que este contexto
 * existe para evitar.
 *
 * <p><strong>El teléfono es el otro dato con cuidado.</strong> Se recibe para mostrar el
 * historial, y solo ese uso: no se guarda en ningún catálogo ni se devuelve a la app en
 * respuestas de disponibilidad, que es donde un dato de una persona no tendría por qué estar.
 *
 * @param reservationId          id de la reserva en la cátedra
 * @param reservationProcessId   id del proceso local asociado
 * @param externalPatientId      usuario dueño de la reserva
 * @param appointmentDate        día del turno
 * @param startTime              hora de inicio
 * @param endTime                hora de fin
 * @param professionalId         profesional
 * @param professionalFirstName  nombre del profesional al momento de reservar
 * @param professionalLastName   apellido del profesional al momento de reservar
 * @param patientFirstName       nombre del paciente al momento de reservar
 * @param patientLastName        apellido del paciente al momento de reservar
 * @param patientPhone           teléfono, nulo si nunca se completó
 * @param status                 estado en la cátedra
 * @param createdAt              cuándo se creó
 * @param confirmedAt            cuándo se confirmó, nulo si no llegó
 * @param cancelledAt            cuándo se canceló, nulo si no se canceló
 * @param cancellationReason     motivo de la cancelación, nulo si no hay
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentResponse(
        long reservationId,
        String reservationProcessId,
        String externalPatientId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        long professionalId,
        String professionalFirstName,
        String professionalLastName,
        String patientFirstName,
        String patientLastName,
        String patientPhone,
        CatedraReservationStatus status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason) {
}
