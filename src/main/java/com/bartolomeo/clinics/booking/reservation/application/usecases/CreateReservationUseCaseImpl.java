package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldExpiredException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldNotFoundException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationAlreadyConfirmedException;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.exception.DuplicateReservationRequestException;
import com.bartolomeo.clinics.booking.reservation.application.exception.MissingIdempotencyKeyException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationHoldUnreconcilableException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.ExternalPatientId;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.CreateReservationUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Inicia un proceso de reserva: retén, confirmación inicial y proceso persistido.
 *
 * <p><strong>El proceso se guarda antes de la primera llamada.</strong> No es una formalidad:
 * es lo que hace que un timeout no deje nada atrás. Si la llamada se pierde, queda un
 * proceso con el turno, el usuario y la clave de la solicitud, que es exactamente lo que
 * hace falta para reconciliar (anexo §18.4) y para que el reintento del cliente no cree una
 * segunda reserva.
 *
 * <p><strong>Repetir la misma solicitud no repite el proceso.</strong> La clave de
 * idempotencia identifica la solicitud: si el proceso ya existe, se sigue desde donde está
 * en lugar de arrancar de cero. Un reintento después de un 503 no vuelve a pedir el turno:
 * reconcilia y sigue.
 *
 * <p><strong>Un timeout no es un fallo.</strong> Un timeout no prueba que la escritura haya
 * fallado, así que antes de repetir una acción con efectos se consulta el estado por los
 * identificadores que ya se guardaron. Para el retén, la consulta son las ocupaciones del
 * turno: si el turno aparece tomado, el retén se creó y no se puede repetir —crearía otro— y
 * no se puede confirmar porque su {@code holdId} se perdió. Para la confirmación, la consulta
 * es el listado de reservas del paciente: si ya hay una reserva {@code PHONE_PENDING} con
 * nuestro proceso, la confirmación había pasado.
 *
 * <p><strong>Lo que no se reintenta a ciegas.</strong> Como máximo un reintento por escritura,
 * y solo después de reconciliar. Dos reintentos sin mirar son dos efectos por ganar uno.
 *
 * <p><strong>El estado del proceso se guarda con lo que dice la cátedra.</strong> El
 * {@code expiresAt} que queda es el suyo, nunca uno calculado acá: la vigencia real la
 * determina la cátedra y una copia local no la extiende (enunciado §7).
 *
 * <p><strong>No hay transacción que abarque las llamadas remotas.</strong> Cada guardado es
 * su propia transacción, y entre guardado y guardado queda una llamada a la cátedra. Una
 * transacción abierta durante un timeout de ocho segundos dejaría bloqueada una fila que
 * todos los demás necesitan.
 */
@Component
@RequiredArgsConstructor
public class CreateReservationUseCaseImpl implements CreateReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateReservationUseCaseImpl.class);

    private final ReservationProcessRepository repository;
    private final CatedraAppointmentsPort catedra;
    private final GetUserUseCase getUser;
    private final ReservationTransitions transitions;
    private final Clock clock;

    @Override
    public ReservationProcess create(String login, ReservationData data) {

        String requestKey = validarClave(data);
        User user = getUser.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));
        ExternalPatientId patientId = ExternalPatientId.ofUser(user.getId());

        Optional<ReservationProcess> existente =
                repository.findByRequestKey(user.getId(), requestKey);

        if (existente.isPresent()) {
            // La misma solicitud, otra vez: se sigue el proceso que ya existe.
            return continuar(existente.get(), user, patientId);
        }

        ReservationProcess proceso;
        try {
            proceso = repository.save(
                    ReservationProcess.initiated(user.getId(), patientId.value(), data, now()));
        } catch (DuplicateReservationRequestException ex) {
            // Dos peticiones con la misma clave entraron al mismo tiempo. El índice único
            // decidió cuál es el bueno, y ese es el que se devuelve: dos procesos para lo
            // mismo es justo lo que la clave de idempotencia debía impedir.
            log.info("Otra petición con la misma clave de idempotencia ganó la carrera");
            return repository.findByRequestKey(user.getId(), requestKey).orElseThrow(() -> ex);
        }

        return continuar(proceso, user, patientId);
    }

    /**
     * Sigue el flujo desde el estado en que esté el proceso.
     *
     * <p>Los dos tramos son independientes y cada uno se hace una sola vez: si el proceso ya
     * tiene retén, no se vuelve a pedir; si ya está esperando el teléfono, no se vuelve a
     * confirmar.
     */
    private ReservationProcess continuar(
            ReservationProcess proceso, User user, ExternalPatientId patientId) {

        ReservationProcess actual = proceso;

        try {
            if (actual.state() == ReservationState.INITIATED) {
                AppointmentHold hold = crearReten(actual);
                actual = repository.save(transitions.holdCreated(
                        actual,
                        hold.holdId(),
                        hold.reservationProcessId(),
                        hold.expiresAt(),
                        now()));
            }

            if (actual.state() == ReservationState.HOLD_CREATED) {
                confirmarReten(actual, user, patientId);
                actual = repository.save(transitions.apply(
                        actual, ReservationEvent.INITIAL_CONFIRMATION, now()));
            }
        } catch (CatedraUnavailableException sinCatedra) {
            // La cátedra no está. El proceso no falló: quedó a medio hacer, y un reintento
            // con la misma clave lo retoma desde acá. Marcarlo como fallido sería mentir
            // sobre el turno.
            log.warn("La cátedra no está disponible: el proceso {} queda a medio hacer",
                    actual.id());
            throw sinCatedra;
        } catch (ApplicationException fallo) {
            throw marcarFallo(actual, fallo);
        }

        return actual;
    }

    /**
     * Pide el retén, reconciliando antes de repetir.
     *
     * <p>El primer intento falla solo si la cátedra no está. Antes de repetir se pregunta si
     * el turno quedó tomado: si quedó, el retén se creó y repetir crearía otro sobre el mismo
     * turno, así que no se repite y el proceso se cierra esperando que el retén venza. Si no
     * quedó tomado, el retén no llegó a crearse y repetir es seguro, una sola vez.
     */
    private AppointmentHold crearReten(ReservationProcess proceso) {
        try {
            return catedra.createHold(
                    proceso.professionalId(),
                    proceso.date(),
                    proceso.startTime(),
                    proceso.externalPatientId());
        } catch (CatedraUnavailableException primerIntento) {
            log.warn("La cátedra no respondió al pedir el retén del turno {} del profesional {}: {}",
                    proceso.date(), proceso.professionalId(), primerIntento.getMessage());

            if (existeRetenEnLaCatedra(proceso)) {
                throw new ReservationHoldUnreconcilableException(
                        "el turno " + proceso.date() + " " + proceso.startTime()
                                + " ya está tomado en la cátedra");
            }

            log.info("El turno {} del profesional {} no quedó tomado: se reintenta el retén",
                    proceso.date(), proceso.professionalId());
            return catedra.createHold(
                    proceso.professionalId(),
                    proceso.date(),
                    proceso.startTime(),
                    proceso.externalPatientId());
        }
    }

    /**
     * Confirma el retén, reconciliando antes de repetir.
     *
     * <p>Repetir la confirmación es seguro: el contrato la define como idempotente y una
     * respuesta que ya estaba confirmada no es un error, es el mismo resultado que se
     * buscaba. Lo que no es seguro es repetirla sin preguntar, y por eso primero se mira si
     * la reserva ya existe.
     */
    private void confirmarReten(
            ReservationProcess proceso, User user, ExternalPatientId patientId) {

        try {
            catedra.confirmHold(
                    proceso.holdId(),
                    patientId.value(),
                    user.getFirstName(),
                    user.getLastName());
        } catch (ReservationAlreadyConfirmedException yaConfirmado) {
            // La confirmación ya había pasado: el resultado es el que se buscaba.
            log.info("El retén {} ya estaba confirmado", proceso.holdId());
        } catch (CatedraUnavailableException primerIntento) {
            log.warn("La cátedra no respondió al confirmar el retén {}: {}",
                    proceso.holdId(), primerIntento.getMessage());

            if (laReservaYaEstaCreada(proceso, patientId)) {
                log.info("La reserva del proceso {} ya existía: la confirmación sí había pasado",
                        proceso.catedraProcessId());
                return;
            }

            log.info("La reserva del proceso {} todavía no existe: se reintenta la confirmación",
                    proceso.catedraProcessId());
            catedra.confirmHold(
                    proceso.holdId(),
                    patientId.value(),
                    user.getFirstName(),
                    user.getLastName());
        }
    }

    /**
     * Si el turno aparece tomado en las ocupaciones de la cátedra.
     *
     * <p>Solo un {@code HELD} cuenta: un {@code CONFIRMED} significa que ya hay una reserva
     * hecha, que es un caso distinto y no uno de retén perdido.
     */
    private boolean existeRetenEnLaCatedra(ReservationProcess proceso) {
        List<Occupancy> ocupaciones = catedra.getOccupancies(
                proceso.professionalId(), proceso.date(), proceso.date());

        return ocupaciones.stream()
                .anyMatch(ocupacion -> ocupacion.date().equals(proceso.date())
                        && ocupacion.startTime().equals(proceso.startTime())
                        && ocupacion.status() == OccupancyStatus.HELD);
    }

    /**
     * Si la reserva del proceso ya existe en la cátedra.
     *
     * <p>Se busca por el proceso de la cátedra, que es el identificador que ella devolvió al
     * crear el retén, y no por el turno: un turno puede tener más de una reserva a lo largo
     * del día, y la que importa es la de este proceso.
     */
    private boolean laReservaYaEstaCreada(
            ReservationProcess proceso, ExternalPatientId patientId) {

        List<CatedraAppointment> reservas = catedra.listAppointments(new AppointmentsQuery(
                patientId.value(), null, proceso.date(), proceso.date(), 0, 20));

        return reservas.stream().anyMatch(reserva ->
                reserva.reservationProcessId() != null
                        && reserva.reservationProcessId().equals(proceso.catedraProcessId())
                        && reserva.status() == CatedraReservationStatus.PHONE_PENDING);
    }

    /**
     * Cierra el proceso que no va a seguir y deja el motivo escrito.
     *
     * <p>Un retén que ya no existe es un proceso vencido; cualquier otro fallo —el turno
     * tomado, el profesional deshabilitado, el slot inválido— es un proceso fallido. En los
     * dos casos el estado es final y el motivo queda para que se pueda explicar después.
     */
    private ApplicationException marcarFallo(ReservationProcess proceso, ApplicationException fallo) {
        ReservationEvent evento =
                (fallo instanceof HoldExpiredException || fallo instanceof HoldNotFoundException)
                        ? ReservationEvent.EXPIRED
                        : ReservationEvent.FAILED;

        repository.save(transitions.apply(proceso, evento, now(), fallo.getMessage()));
        return fallo;
    }

    /** La clave de idempotencia es obligatoria y no puede estar en blanco. */
    private String validarClave(ReservationData data) {
        if (data.requestKey() == null || data.requestKey().isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        return data.requestKey().trim();
    }

    private Instant now() {
        return clock.instant();
    }
}
