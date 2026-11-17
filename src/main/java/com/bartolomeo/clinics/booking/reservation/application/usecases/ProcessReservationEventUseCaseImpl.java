package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.reservation.application.exception.UnknownReservationProcessException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEventEnvelope;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ProcessedEventRepository;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica un evento de la cátedra al proceso que le corresponde.
 *
 * <p><strong>La idempotencia va por el {@code eventId}.</strong> Kafka entrega al menos una
 * vez, así que el mismo mensaje puede llegar dos veces: la segunda vez el evento ya está en
 * la lista de procesados y no vuelve a tocar el proceso. Es la evidencia de idempotencia que
 * pide el enunciado.
 *
 * <p><strong>El evento se busca por el proceso de la cátedra.</strong> Es el identificador
 * que ella da y el único que aparece en el mensaje. Un evento de un proceso que este
 * servicio no conoce no se aplica a ninguno: se rechaza, porque aplicarlo a cualquier otro
 * proceso sería inventar a quién pertenece.
 *
 * <p><strong>El efecto y la anotación del evento van en la misma transacción.</strong> O se
 * aplica el evento y se anota, o no se hace ninguna de las dos cosas. Si se anotara antes,
 * una caída entre las dos cosas dejaría el evento marcado como aplicado sin haberlo aplicado,
 * y nunca volvería a llegar.
 *
 * <p><strong>Los resultados finales dejan además la reserva guardada.</strong> Una
 * confirmación no es solo un estado: es un turno tomado de verdad, y el usuario tiene que
 * poder verla después. La reserva guarda el identificador que le dio la cátedra y el turno
 * del proceso; los datos descriptivos los completa la reconciliación por REST, porque son
 * historia y no el catálogo vigente.
 */
@Component
@RequiredArgsConstructor
public class ProcessReservationEventUseCaseImpl implements ProcessReservationEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessReservationEventUseCaseImpl.class);

    private static final String ESTADO_CONFIRMADA = "CONFIRMED";
    private static final String ESTADO_CANCELADA = "CANCELLED";

    private final ReservationProcessRepository repository;
    private final ReservationRepository reservations;
    private final ProcessedEventRepository processedEvents;
    private final ReservationTransitions transitions;
    private final Clock clock;

    @Override
    @Transactional
    public void process(ReservationEventEnvelope envelope) {

        if (processedEvents.existsByEventId(envelope.eventId())) {
            // Ya se aplico una vez. Repetir el efecto seria un error, no una precaucion.
            return;
        }

        ReservationProcess proceso = repository
                .findByCatedraProcessId(envelope.reservationProcessId())
                .orElseThrow(() -> new UnknownReservationProcessException(
                        envelope.reservationProcessId()));

        ReservationProcess actualizado = envelope.event() == ReservationEvent.INITIAL_CONFIRMATION
                // La cátedra pide el teléfono con un evento propio, y ese evento es el que
                // hay que responder después: su identificador queda guardado en el proceso.
                ? transitions.waitingForPhone(proceso, envelope.eventId(), clock.instant())
                : transitions.apply(
                        proceso, envelope.event(), clock.instant(), envelope.reason());

        repository.save(actualizado);
        registraElResultadoFinal(actualizado, envelope);
        processedEvents.save(envelope.eventId(), envelope.schemaVersion());
    }

    /**
     * Guarda la reserva cuando el evento es un resultado final.
     *
     * <p>Una confirmación crea la reserva y una cancelación la cambia de estado. Los demás
     * eventos no dejan reserva: un proceso que espera el teléfono todavía no tiene turno
     * tomado.
     */
    private void registraElResultadoFinal(
            ReservationProcess proceso, ReservationEventEnvelope envelope) {

        if (envelope.event() == ReservationEvent.CONFIRMED) {
            guardaLaReserva(proceso, envelope);
        } else if (envelope.event() == ReservationEvent.CANCELLED) {
            cancelaLaReserva(proceso);
        }
    }

    /**
     * Crea la reserva confirmada.
     *
     * <p>Si ya había una reserva para ese proceso, no se crea otra: el proceso es único y
     * la reserva también.
     */
    private void guardaLaReserva(
            ReservationProcess proceso, ReservationEventEnvelope envelope) {

        if (reservations.findByProcessId(proceso.id()).isPresent()) {
            return;
        }

        Instant ahora = clock.instant();
        Reservation reserva = new Reservation(
                null,
                envelope.catedraReservationId() == null ? 0L : envelope.catedraReservationId(),
                proceso.id(),
                proceso.userId(),
                proceso.professionalId(),
                proceso.date(),
                proceso.startTime(),
                null,
                ESTADO_CONFIRMADA,
                null,
                null,
                null,
                null,
                ahora);

        reservations.save(reserva);
        log.info("Reserva confirmada para el proceso {} (reserva {} en la catedra)",
                proceso.id(), reserva.catedraReservationId());
    }

    /** Cancela la reserva que había, si la había. */
    private void cancelaLaReserva(ReservationProcess proceso) {
        Optional<Reservation> existente = reservations.findByProcessId(proceso.id());
        if (existente.isEmpty()) {
            return;
        }
        reservations.save(existente.get().conEstado(ESTADO_CANCELADA));
    }
}
