package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidReservationTransitionException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException;
import com.bartolomeo.clinics.booking.reservation.application.service.ReservationTransitions;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.CancelReservationUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela una reserva del usuario de la sesión.
 *
 * <p><strong>Cancelar dos veces devuelve el estado actual.</strong> La segunda vez el proceso
 * ya está cancelado y se devuelve tal cual, sin volver a llamar a la cátedra: el botón de
 * cancelar del móvil puede pulsarse dos veces sin que la segunda rompa nada.
 *
 * <p><strong>La cancelación real la hace la cátedra.</strong> Este servicio es el dueño del
 * proceso local, pero el turno tomado vive del otro lado: cancelar acá sin cancelar allá
 * dejaría al usuario con un turno reservado que no existe.
 *
 * <p><strong>Solo el dueño puede cancelar.</strong> La verificación es parte de la consulta:
 * un proceso de otro usuario no se encuentra.
 */
@Component
@RequiredArgsConstructor
public class CancelReservationUseCaseImpl implements CancelReservationUseCase {

    private final ReservationProcessRepository repository;
    private final CatedraAppointmentsPort catedra;
    private final GetUserUseCase getUser;
    private final ReservationTransitions transitions;
    private final Clock clock;

    @Override
    @Transactional
    public ReservationProcess cancel(long processId, String login, String reason) {

        User user = getUser.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));

        ReservationProcess proceso = repository.findById(processId)
                .filter(candidato -> candidato.userId().equals(user.getId()))
                .orElseThrow(() -> new ReservationProcessNotFoundException(processId));

        if (proceso.state() == ReservationState.CANCELLED) {
            // Ya está cancelado: se devuelve el estado actual y no se repite nada.
            return proceso;
        }

        if (proceso.catedraProcessId() == null) {
            // Todavía no hay nada en la cátedra que cancelar: el proceso ni siquiera
            // llegó a tomar el retén.
            throw new InvalidReservationTransitionException(
                    proceso.state(), ReservationEvent.CANCELLED);
        }

        // Primero se comprueba que la cancelación sea posible. Un proceso cerrado no se
        // cancela, y no tiene sentido avisarle a la cátedra de una cancelación imposible.
        Optional<ReservationProcess> nuevo =
                proceso.apply(ReservationEvent.CANCELLED, clock.instant());
        if (nuevo.isEmpty()) {
            throw new InvalidReservationTransitionException(
                    proceso.state(), ReservationEvent.CANCELLED);
        }

        // La cátedra cancela. Su cancelación es idempotente: si la reserva ya estaba
        // cancelada, devuelve el estado actual en vez de fallar.
        catedra.cancelAppointment(proceso.catedraProcessId(), reason);

        return repository.save(nuevo.get());
    }
}
