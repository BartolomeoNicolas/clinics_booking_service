package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

/**
 * Cuerpo de {@code POST /api/appointment-holds/{holdId}/confirm}.
 *
 * <p><strong>El paciente es obligatorio y tiene que coincidir con el que se usó al crear el
 * retén</strong>, que es la regla que impide confirmar un turno reservado por otro. Los
 * nombres van acá porque es donde la cátedra los necesita para crear la reserva; el teléfono
 * no, porque todavía no lo tiene —ese es justamente el paso que sigue.
 *
 * @param externalPatientId   debe coincidir con el del retén, hasta 100 caracteres
 * @param patientFirstName    nombre, de 2 a 100 caracteres
 * @param patientLastName     apellido, de 2 a 100 caracteres
 */
public record AppointmentHoldConfirmationRequest(
        String externalPatientId,
        String patientFirstName,
        String patientLastName) {
}
