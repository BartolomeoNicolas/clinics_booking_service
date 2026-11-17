package com.bartolomeo.clinics.booking.user.domain.ports.in;

import com.bartolomeo.clinics.booking.user.domain.model.User;

import java.util.Optional;

/**
 * Busca un usuario por su login.
 */
public interface GetUserUseCase {

    Optional<User> findByLogin(String login);
}
