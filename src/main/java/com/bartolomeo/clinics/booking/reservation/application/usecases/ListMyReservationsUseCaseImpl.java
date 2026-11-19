package com.bartolomeo.clinics.booking.reservation.application.usecases;

import com.bartolomeo.clinics.booking.reservation.domain.model.PageResult;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ListMyReservationsUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.GetUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Las reservas del usuario de la sesión.
 *
 * <p><strong>El usuario sale del token y no de la consulta.</strong> No hay un parámetro que
 * diga "de quién": la única forma de obtener las reservas de otro usuario es su token.
 *
 * <p><strong>Lo que devuelve la cátedra son las de toda la cuenta técnica.</strong> La
 * cuenta técnica es de la cátedra, no del usuario, así que su listado trae las reservas de
 * todos los alumnos. Este servicio nunca las muestra todas: pide las del usuario y descarta
 * el resto.
 */
@Component
@RequiredArgsConstructor
public class ListMyReservationsUseCaseImpl implements ListMyReservationsUseCase {

    /** Tope de página: un usuario con miles de reservas no es un caso real. */
    private static final int MAXIMO_DE_PAGINA = 100;

    private final ReservationProcessRepository repository;
    private final GetUserUseCase getUser;

    @Override
    public PageResult<ReservationProcess> list(String login, int page, int size) {

        User user = getUser.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));

        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), MAXIMO_DE_PAGINA);

        java.util.List<ReservationProcess> contenido =
                repository.findByUserId(user.getId(), pagina, tamano);
        long total = repository.countByUserId(user.getId());

        return new PageResult<>(contenido, pagina, tamano, total);
    }
}
