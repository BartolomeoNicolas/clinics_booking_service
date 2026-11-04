package com.bartolomeo.clinics.booking.catedra.domain.ports.out;

import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentCancellation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentConfirmation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lo que este servicio le pide a la cátedra para resolver una reserva.
 *
 * <p><strong>Es el único camino de salida hacia la cátedra, y no hay otro.</strong> Los casos de
 * uso de {@code availability} y de {@code reservation} inyectan este puerto; lo que hay del
 * otro lado es HTTP, un JWT técnico y una matriz de errores. Ninguno de ellos sabe cómo se
 * consigue ese JWT ni qué hace la cátedra cuando el hold ya venció.
 *
 * <p><strong>Sin efectos locales.</strong> Estos métodos no guardan nada: el proceso, el
 * {@code holdId} y la vigencia se persisten en {@code reservation}, que es el dueño del proceso
 * al que pertenecen. Un cliente no acumula estado.
 *
 * <p><strong>Sobre reintentar.</strong> Las dos lecturas —ocupaciones y listado— se pueden
 * repetir solas si fallan, porque repetirlas no cambia nada en el mundo. Las tres escrituras no:
 * crear un retén dos veces puede dejar dos, y aunque confirmar y cancelar son idempotentes según
 * el contrato, un timeout <em>no prueba</em> que la operación haya fallado (anexo 18.4), así que
 * repetirla a ciegas sería arriesgarse a dos efectos para ganar un reintento. Quien reintenta
 * una escritura es el caso de uso, mirando primero el estado por los identificadores que ya
 * guardó.
 *
 * <p>Todos los errores —404, 409, 403, 401, 5xx, caída de red— llegan traducidos a excepciones
 * de {@code catedra.application.exception}. Ninguna excepción del framework cruza este puerto.
 */
public interface CatedraAppointmentsPort {

    /**
     * Posiciones no disponibles de un profesional entre dos fechas, ambas inclusivas.
     *
     * @param professionalId profesional consultado
     * @param from           primera fecha inclusiva
     * @param to             última fecha inclusiva, no puede ser anterior a {@code from}
     * @return las ocupaciones del rango. Puede ser vacía: es una respuesta válida
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.InvalidDateRangeException
     *         si {@code from} es posterior a {@code to}
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     *         si la cátedra no está disponible
     */
    List<Occupancy> getOccupancies(long professionalId, LocalDate from, LocalDate to);

    /**
     * Reserva temporal un turno.
     *
     * @param professionalId  profesional del turno
     * @param date            día del turno
     * @param startTime       hora de inicio, en {@code HH:mm} como manda el contrato
     * @param externalPatientId identificador del usuario, tomado del mapeo local del usuario y
     *                          nunca de lo que manda el cliente
     * @return el retén, con su vigencia real
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ProfessionalNotFoundException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ProfessionalDisabledException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.InvalidSlotException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.SlotAlreadyReservedException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.SlotAlreadyHeldException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     */
    AppointmentHold createHold(
            long professionalId, LocalDate date, LocalTime startTime, String externalPatientId);

    /**
     * Confirma inicialmente un retén y pone el proceso a la espera del teléfono.
     *
     * @param holdId              retén a confirmar
     * @param externalPatientId   debe coincidir con el que se usó al crearlo
     * @param patientFirstName    nombre del paciente, entre 2 y 100 caracteres
     * @param patientLastName     apellido del paciente, entre 2 y 100 caracteres
     * @return la confirmación, con el límite antes del cual hay que completar el teléfono
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.HoldNotFoundException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.HoldExpiredException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.HoldOwnerMismatchException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ExternalPatientMismatchException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ReservationAlreadyConfirmedException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     */
    AppointmentConfirmation confirmHold(
            String holdId, String externalPatientId, String patientFirstName,
            String patientLastName);

    /**
     * Lista reservas de la cuenta técnica.
     *
     * <p><strong>La lista no viene filtrada por usuario</strong> —la cátedra no los distingue—,
     * y este método tampoco lo hace: filtrar es trabajo del caso de uso que la pide, que es el
     * que sabe de qué usuario habla.
     *
     * <p>La respuesta trae además {@code X-Total-Count} y {@code Link}, que todavía no se
     * superficie: la paginación completa —recorrer todas las páginas— la necesita quien
     * reconcilia, y ese caso de uso todavía no existe. Se deja escrito para que no parezca que
     * no estaban en el contrato.
     *
     * @param query criterios, todos opcionales
     * @return las reservas de la página pedida. Puede ser vacía
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     */
    List<CatedraAppointment> listAppointments(AppointmentsQuery query);

    /**
     * Cancela una reserva.
     *
     * <p>El motivo es opcional y puede ser nulo. Cancelar una reserva ya cancelada devuelve su
     * estado actual en vez de fallar.
     *
     * @param reservationProcessId proceso a cancelar
     * @param reason               motivo, hasta 500 caracteres; puede ser nulo
     * @return el estado final de la reserva
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ReservationNotFoundException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ReservationOwnerMismatchException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.ReservationNotCancellableException
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     */
    AppointmentCancellation cancelAppointment(String reservationProcessId, String reason);
}
