package com.bartolomeo.clinics.booking.user.domain.ports.out;

import com.bartolomeo.clinics.booking.user.domain.model.User;

import java.util.Optional;

/**
 * Persistencia de los usuarios.
 *
 * <p>Nombre de repositorio, no de puerto, como en el repo de referencia. Habla de usuarios del
 * dominio: no conoce entidades ni JPA.
 */
public interface UserRepository {

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    User save(User user);
}
