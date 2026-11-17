package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationRegistrar;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ReconcileReservationsUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Converge los procesos pendientes con el estado real de la cátedra.
 *
 * <p><strong>Un reinicio no puede dejar procesos imposibles.</strong> Si el servicio se apaga
 * con procesos a medio hacer, al volver tiene que saber en qué estado quedó cada uno. Para eso
 * pregunta a la cátedra —que es la dueña de la verdad— y mueve el proceso local a donde ella
 * dice, aunque el evento que lo decía se haya perdido.
 *
 * <p><strong>La vigencia la manda la cátedra.</strong> Un proceso cuyo vencimiento ya pasó
 * se cierra como vencido sin preguntar nada: no hay nada que reconciliar, hay una ventana
 * que se cerró.
 *
 * <p><strong>Lo que todavía puede pasar no se toca.</strong> Un proceso que solo llegó a
 * tomar el retén puede seguir esperando: la cátedra no lista los retenes —solo las
 * reservas—, así que su ausencia en el listado no prueba nada. Solo se cierra el proceso que
 * esperaba una reserva, porque esa tenía que existir.
 *
 * <p><strong>Un fallo no corta el barrido.</strong> Si la cátedra no responde, el proceso que
 * se estaba revisando se deja como estaba y se sigue con el siguiente: un barrido a medias es
 * mejor que un barrido que no mira nada.
 */
@Component
@RequiredArgsConstructor
public class ReconcileReservationsUseCaseImpl implements ReconcileReservationsUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(ReconcileReservationsUseCaseImpl.class);

    /** Cuántas reservas se piden por proceso: las de un usuario en un día son pocas. */
    private static final int TAMANO_DE_PAGINA = 100;

    private final ReservationProcessRepository repository;
    private final CatedraAppointmentsPort catedra;
    private final ReservationTransitions transitions;
    private final ReservationRegistrar registrar;
    private final Clock clock;

    @Override
    public int reconcilePending() {

        int convergidos = 0;
        for (ReservationProcess proceso : repository.findOpen()) {
            try {
                if (reconcilia(proceso)) {
                    convergidos++;
                }
            } catch (RuntimeException ex) {
                // Un proceso que no se pudo revisar no impide revisar los demás.
                log.warn("No se pudo reconciliar el proceso {}: {}",
                        proceso.id(), ex.getMessage());
            }
        }
        return convergidos;
    }

    /**
     * Reconcilia un proceso.
     *
     * @return {@code true} si el proceso cambió de estado
     */
    private boolean reconcilia(ReservationProcess proceso) {

        Instant now = clock.instant();

        if (proceso.expiresAt() != null && !now.isBefore(proceso.expiresAt())) {
            // La ventana se cerró: no hay nada que preguntar.
            return aplica(proceso, ReservationEvent.EXPIRED, "El proceso venció").isPresent();
        }

        Optional<CatedraAppointment> propia = reservaEnLaCatedra(proceso);

        if (propia.isEmpty()) {
            return cierraSiEsperabaUnaReserva(proceso);
        }

        return converge(proceso, propia.get());
    }

    /**
     * Si el proceso esperaba una reserva que la cátedra no tiene, se cae.
     *
     * <p>Un proceso que esperaba el teléfono o el resultado final tenía una reserva en la
     * cátedra: si no está, el retén venció. Uno que solo llegó al retén puede seguir
     * esperando: los retenes no se listan y su ausencia no prueba nada.
     */
    private boolean cierraSiEsperabaUnaReserva(ReservationProcess proceso) {
        boolean esperabaReserva = proceso.state() == ReservationState.WAITING_FOR_PHONE
                || proceso.state() == ReservationState.PHONE_SUBMITTED;

        if (!esperabaReserva) {
            return false;
        }
        return aplica(proceso, ReservationEvent.EXPIRED,
                "La cátedra no conoce la reserva del proceso").isPresent();
    }

    /** Mueve el proceso al estado en que la cátedra dice que está. */
    private boolean converge(ReservationProcess proceso, CatedraAppointment reserva) {

        CatedraReservationStatus estado = reserva.status();

        if (estado == CatedraReservationStatus.PHONE_PENDING) {
            // La reserva existe y sigue esperando: no hay nada que mover.
            return false;
        }

        if (estado == CatedraReservationStatus.CONFIRMED) {
            Optional<ReservationProcess> nuevo = lleva(proceso,
                    ReservationEvent.HOLD_CREATED,
                    ReservationEvent.INITIAL_CONFIRMATION,
                    ReservationEvent.CONFIRMED);

            nuevo.ifPresent(p -> registrar.registraConfirmacion(p, reserva.reservationId()));
            return nuevo.isPresent();
        }

        if (estado == CatedraReservationStatus.CANCELLED) {
            Optional<ReservationProcess> nuevo =
                    lleva(proceso, ReservationEvent.CANCELLED);
            nuevo.ifPresent(registrar::registraCancelacion);
            return nuevo.isPresent();
        }

        // La cátedra dio la reserva por fallida.
        return aplica(proceso, ReservationEvent.FAILED,
                "La cátedra dio la reserva por fallida").isPresent();
    }

    /**
     * Aplica un camino de eventos, saltando los que no apliquen.
     *
     * <p>El camino completo es el que la cátedra recorrió: tomar el retén, la confirmación
     * inicial y la final. Un proceso local puede estar en cualquiera de los puntos de ese
     * camino —o en ninguno, si se apagó justo antes— y lo que importa es llegar al final.
     * Un paso que no aplica desde el estado en que está el proceso se salta y se sigue con
     * el siguiente: el camino es el recorrido de la cátedra, no una lista de pasos
     * obligatorios.
     */
    private Optional<ReservationProcess> lleva(
            ReservationProcess proceso, ReservationEvent... camino) {

        Optional<ReservationProcess> actual = Optional.of(proceso);
        boolean cambio = false;

        for (ReservationEvent evento : camino) {
            Optional<ReservationProcess> nuevo =
                    actual.get().apply(evento, clock.instant(), null);
            if (nuevo.isPresent()) {
                actual = nuevo;
                cambio = true;
            }
        }

        if (cambio) {
            repository.save(actual.get());
        }
        return cambio ? actual : Optional.empty();
    }

    /** Aplica un evento y guarda el proceso si la transición existe. */
    private Optional<ReservationProcess> aplica(
            ReservationProcess proceso, ReservationEvent evento, String motivo) {

        Optional<ReservationProcess> nuevo = proceso.apply(evento, clock.instant(), motivo);
        nuevo.ifPresent(repository::save);
        return nuevo;
    }

    /**
     * La reserva de este proceso en la cátedra, si está.
     *
     * <p><strong>Se busca por el identificador de la cátedra.</strong> Es el que ella dio y
     * el que no se confunde con nada. Cuando el proceso local todavía no lo tiene —se apagó
     * antes de guardarlo— se busca por el turno, que es único: un turno tiene una sola
     * reserva.
     */
    private Optional<CatedraAppointment> reservaEnLaCatedra(ReservationProcess proceso) {
        List<CatedraAppointment> reservas = catedra.listAppointments(new AppointmentsQuery(
                proceso.externalPatientId(), null, proceso.date(), proceso.date(), 0, TAMANO_DE_PAGINA));

        return reservas.stream()
                .filter(reserva -> coincide(reserva, proceso))
                .findFirst();
    }

    private boolean coincide(CatedraAppointment reserva, ReservationProcess proceso) {
        if (proceso.catedraProcessId() != null
                && proceso.catedraProcessId().equals(reserva.reservationProcessId())) {
            return true;
        }
        // El proceso local todavía no conoce el identificador de la cátedra: se busca por el
        // turno, que no puede tener dos reservas.
        return reserva.professionalId() == proceso.professionalId()
                && reserva.appointmentDate() != null
                && reserva.appointmentDate().equals(proceso.date())
                && reserva.startTime() != null
                && reserva.startTime().equals(proceso.startTime());
    }
}
