package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidPhoneNumberException;
import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationExpiredException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneNumber;
import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneSubmission;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.SubmitPhoneUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Manda el teléfono de un proceso que está esperando la información.
 *
 * <p><strong>Primero se valida todo y recién después se publica.</strong> El teléfono se
 * normaliza y se comprueba contra el formato que acepta la cátedra, el proceso tiene que
 * ser del usuario que pregunta, tiene que estar esperando la información y no puede haber
 * vencido. Recién con todo eso en regla sale el mensaje: un teléfono inválido no puede
 * dejar medio publicado nada.
 *
 * <p><strong>Nunca se publica fuera de la ventana.</strong> El vencimiento es el de la
 * cátedra y manda: si ya pasó, el proceso se cierra como vencido y el mensaje no sale,
 * porque un mensaje de un proceso vencido llega tarde y no lo atiende nadie.
 *
 * <p><strong>Cada envío es un mensaje nuevo.</strong> El identificador del mensaje se
 * genera acá, en cada intento, y el que se referencia es el del pedido vigente: el evento
 * que la cátedra mandó pidiendo la información. Así un reenvío después de un rechazo es
 * una respuesta nueva que además apunta al pedido correcto.
 *
 * <p><strong>Publicar y guardar van en ese orden.</strong> Si el guardado fallara después
 * de publicar, el proceso quedaría esperando el teléfono y el usuario no podría mandarlo
 * otra vez. Al revés, el proceso quedaría diciendo que el teléfono salió cuando todavía no
 * salió, que es un error que se puede corregir reintentando.
 */
@Component
@RequiredArgsConstructor
public class SubmitPhoneUseCaseImpl implements SubmitPhoneUseCase {

    private final ReservationProcessRepository repository;
    private final PhoneSubmissionPublisher publisher;
    private final GetUserUseCase getUser;
    private final ReservationTransitions transitions;
    private final Clock clock;

    @Override
    public ReservationProcess submitPhone(long processId, String login, String phone) {

        User user = getUser.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));

        PhoneNumber numero = PhoneNumber.of(phone)
                .orElseThrow(() -> new InvalidPhoneNumberException(phone));

        ReservationProcess proceso = repository.findById(processId)
                .filter(candidato -> candidato.userId().equals(user.getId()))
                .orElseThrow(() -> new ReservationProcessNotFoundException(processId));

        Instant now = clock.instant();

        if (proceso.expiresAt() != null && !now.isBefore(proceso.expiresAt())) {
            // La ventana ya se cerró: el proceso queda vencido y no sale ningún mensaje.
            repository.save(transitions.apply(proceso, ReservationEvent.EXPIRED, now));
            throw new ReservationExpiredException(proceso.expiresAt());
        }

        if (proceso.requestEventId() == null) {
            // Todavía no hay un pedido de la cátedra que responder. Sin esa referencia el
            // mensaje saldría sin saber a qué pedido contesta.
            throw new InvalidReservationTransitionException(
                    proceso.state(), ReservationEvent.PHONE_SUBMITTED);
        }

        // La transición se valida antes de publicar: si el proceso no está esperando el
        // teléfono, no se publica nada.
        ReservationProcess actualizado =
                transitions.apply(proceso, ReservationEvent.PHONE_SUBMITTED, now);

        PhoneSubmission submission = new PhoneSubmission(
                UUID.randomUUID().toString(),
                proceso.catedraProcessId(),
                proceso.holdId(),
                proceso.requestEventId(),
                numero.value());

        publisher.publish(submission);

        return repository.save(actualizado);
    }
}
