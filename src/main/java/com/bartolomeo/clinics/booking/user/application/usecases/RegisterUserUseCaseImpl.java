package com.bartolomeo.clinics.booking.user.application.usecases;

import com.bartolomeo.clinics.booking.user.application.exception.EmailAlreadyTakenException;
import com.bartolomeo.clinics.booking.user.application.exception.LoginAlreadyTakenException;
import com.bartolomeo.clinics.booking.user.domain.model.RegistrationData;
import com.bartolomeo.clinics.booking.user.domain.model.Role;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.RegisterUserUseCase;
import com.bartolomeo.clinics.booking.user.domain.ports.out.PasswordEncoderPort;
import com.bartolomeo.clinics.booking.user.domain.ports.out.UserRepository;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registra un usuario final.
 *
 * <p>La unicidad de login y email es una regla del negocio, asi que el error se lanza aca y
 * no en la fachada: la fachada traduce <em>ausencia</em> a excepcion, pero la ausencia no
 * distingue si fallo el login o el email, y el cliente necesita saberlo.
 */
@Component
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository repository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    public User register(RegistrationData data) {

        if (repository.existsByLogin(data.getLogin())) {
            throw new LoginAlreadyTakenException(data.getLogin());
        }

        if (repository.existsByEmail(data.getEmail())) {
            throw new EmailAlreadyTakenException(data.getEmail());
        }

        User user = User.builder()
                .login(data.getLogin())
                .firstName(data.getFirstName())
                .lastName(data.getLastName())
                .email(data.getEmail())
                .imageUrl(data.getImageUrl())
                .langKey(data.getLangKey() == null || data.getLangKey().isBlank()
                        ? "es" : data.getLangKey())
                .activated(true)
                .roles(List.of(Role.ROLE_USER))
                .build();

        return repository.save(withHashedPassword(user, data.getPassword()));
    }

    private User withHashedPassword(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }
}
