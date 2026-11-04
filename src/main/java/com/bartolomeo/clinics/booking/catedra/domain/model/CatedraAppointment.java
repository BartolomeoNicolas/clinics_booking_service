package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una reserva tal como la lista la cátedra en {@code GET /api/appointments}.
 *
 * <p><strong>La lista es de toda la cuenta técnica y este servicio la tiene que filtrar.</strong>
 * La cátedra devuelve las reservas de todos los usuarios del proyecto, porque no sabe que
 * existen; el aislamiento entre usuarios es responsabilidad de booking y ocurre al consultar,
 * no allá. Ningún caso de uso puede servir esta lista cruda a la app.
 *
 * <p>Los campos descriptivos —nombre del profesional y del paciente, teléfono— se reciben para
 * dos cosas muy distintas y conviene no confundirlas: mostrar lo que pasó en el historial, y
 * <em>no</em> usarlos como si fueran el catálogo vigente. El nombre de un profesional se
 * consulta siempre contra el catálogo; el que está acá es una fotografía del momento de la
 * reserva, y por eso está acá y no en el catálogo.
 *
 * @param reservationId          id de la reserva en la cátedra
 * @param reservationProcessId   id del proceso local asociado
 * @param externalPatientId      usuario dueño de la reserva, en la forma que la cátedra lo conoce
 * @param appointmentDate        día del turno
 * @param startTime              hora de inicio
 * @param endTime                hora de fin
 * @param professionalId         profesional
 * @param professionalFirstName  nombre del profesional al momento de reservar
 * @param professionalLastName   apellido del profesional al momento de reservar
 * @param patientFirstName       nombre del paciente al momento de reservar
 * @param patientLastName        apellido del paciente al momento de reservar
 * @param patientPhone           teléfono, si se llegó a completar
 * @param status                 estado en la cátedra
 * @param createdAt              cuándo se creó
 * @param confirmedAt            cuándo se confirmó, puede ser nulo
 * @param cancelledAt            cuándo se canceló, puede ser nulo
 * @param cancellationReason     motivo de cancelación, puede ser nulo
 */
public record CatedraAppointment(
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
