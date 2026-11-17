package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.mapper;

import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentCancellation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentConfirmation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentCancellationResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldConfirmationResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.OccupanciesResponse;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;

/**
 * Del hilo de cable del contrato a lo que este servicio entiende.
 *
 * <p>Es un espejo plano, sin reglas de negocio: los tipos del hilo de cable existen para
 * reflejar el JSON tal cual viene —con el {@code id_token} que trae la cátedra, con los
 * campos que ignoramos—, y los del dominio existen para que nadie del otro lado tenga que
 * preguntarse qué significa algo. El salto entre uno y otro es este archivo, y no está en el
 * adaptador, para que leer el adaptador sea leer la llamada y no también la traducción.
 *
 * <p><strong>Lo único que no se copia tal cual son las horas de la agenda</strong>, que vienen
 * como texto {@code HH:mm:ss} y se convierten a {@link java.time.LocalTime}: es la misma
 * hora, solo escrita como la escribe este servicio.
 */
public final class CatedraRestMapper {

    private CatedraRestMapper() {
    }

    public static Occupancy toOccupancy(OccupanciesResponse.OccupancyItem item) {
        if (item.date() == null || item.startTime() == null || item.status() == null) {
            // Una ocupación sin día ni hora de inicio no dice en qué lugar de la agenda está,
            // y no emparejarla con ningún turno pasaría por un hueco que efectivamente está
            // tomado. Se corta acá, en el borde del contrato: del otro lado no se puede
            // arreglar algo que no se entiende, y quedarse tranquilamente con lo que sí se
            // entiende sería irse con la mitad de la lista de ocupaciones.
            throw new CatedraUnavailableException(
                    "La cátedra devolvió una ocupación incompleta en "
                            + item.date() + " " + item.startTime() + ".");
        }
        return new Occupancy(item.date(), item.startTime(), item.status(), item.expiresAt());
    }

    public static AppointmentHold toHold(AppointmentHoldResponse response) {
        return new AppointmentHold(
                response.holdId(),
                response.reservationProcessId(),
                response.status(),
                response.expiresAt());
    }

    public static AppointmentConfirmation toConfirmation(
            AppointmentHoldConfirmationResponse response) {
        return new AppointmentConfirmation(
                response.reservationProcessId(),
                response.status(),
                response.expiresAt());
    }

    public static CatedraAppointment toAppointment(AppointmentResponse response) {
        if (response.status() == null) {
            // Un estado que no está en la enumeración es una violación del contrato, no una
            // reserva rara: no se devuelve como si fuera un estado más, porque quien reciba
            // esto después tiene que poder confiar en que todos los estados se entienden.
            throw new CatedraUnavailableException(
                    "La cátedra devolvió una reserva con un estado fuera del contrato: "
                            + response.reservationProcessId() + ".");
        }
        return new CatedraAppointment(
                response.reservationId(),
                response.reservationProcessId(),
                response.externalPatientId(),
                response.appointmentDate(),
                response.startTime(),
                response.endTime(),
                response.professionalId(),
                response.professionalFirstName(),
                response.professionalLastName(),
                response.patientFirstName(),
                response.patientLastName(),
                response.patientPhone(),
                response.status(),
                response.createdAt(),
                response.confirmedAt(),
                response.cancelledAt(),
                response.cancellationReason());
    }

    public static AppointmentCancellation toCancellation(AppointmentCancellationResponse response) {
        return new AppointmentCancellation(
                response.reservationId(),
                response.reservationProcessId(),
                response.status(),
                response.cancelledAt());
    }
}
