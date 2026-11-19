package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationProcessNotFoundException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.GetReservationProcessUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * El estado de una reserva del usuario de la sesión.
 *
 * <p><strong>La verificación de propietario es parte de la consulta, no un filtro
 * después.</strong> El proceso se busca por identificador y por usuario en la misma
 * operación: un proceso de otro usuario no se encuentra, y por lo tanto no existe.
 */
@Component
@RequiredArgsConstructor
public class GetReservationProcessUseCaseImpl implements GetReservationProcessUseCase {

    private final ReservationProcessRepository repository;
    private final GetUserUseCase getUser;

    @Override
    public ReservationProcess get(long processId, String login) {

        User user = getUser.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));

        return repository.findById(processId)
                .filter(proceso -> proceso.userId().equals(user.getId()))
                .orElseThrow(() -> new ReservationProcessNotFoundException(processId));
    }
}
