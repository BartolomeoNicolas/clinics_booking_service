package com.bartolomeo.clinics.booking.user.application.usecases;

import com.bartolomeo.clinics.booking.user.application.exception.InvalidCredentialsException;
import com.bartolomeo.clinics.booking.user.domain.model.LoginCredentials;
import com.bartolomeo.clinics.booking.user.domain.model.Session;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.LoginUseCase;
import com.bartolomeo.clinics.booking.user.domain.ports.out.PasswordEncoderPort;
import com.bartolomeo.clinics.booking.user.domain.ports.out.TokenIssuerPort;
import com.bartolomeo.clinics.booking.user.domain.ports.out.UserRepository;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Inicia sesion.
 *
 * <p>Un login inexistente y una contrasena incorrecta dan el MISMO error: distinguir los dos
 * le diria a un atacante que logins existen.
 */
@Component
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository repository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenIssuerPort tokenIssuer;

    @Override
    public Session login(LoginCredentials credentials) {

        Optional<User> found = repository.findByLogin(credentials.getLogin());

        if (found.isEmpty() || !passwordEncoder.matches(
                credentials.getPassword(), found.get().getPasswordHash())) {
            throw new InvalidCredentialsException(credentials.getLogin());
        }

        User user = found.get();

        if (!user.isActivated()) {
            throw new InvalidCredentialsException(credentials.getLogin());
        }

        TokenIssuerPort.IssuedToken issued = tokenIssuer.issue(user);

        return Session.builder()
                .token(issued.token())
                .user(user)
                .expiresAt(issued.expiresAt())
                .build();
    }
}
