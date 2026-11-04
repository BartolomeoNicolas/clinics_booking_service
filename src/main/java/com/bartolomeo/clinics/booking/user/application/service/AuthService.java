package com.bartolomeo.clinics.booking.user.application.service;

import com.bartolomeo.clinics.booking.user.application.exception.UserNotFoundException;
import com.bartolomeo.clinics.booking.user.domain.model.LoginCredentials;
import com.bartolomeo.clinics.booking.user.domain.model.RegistrationData;
import com.bartolomeo.clinics.booking.user.domain.model.Session;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.*;
import lombok.*;
import org.springframework.stereotype.Service;

/**
 * Fachada del contexto de usuarios.
 *
 * <p>Es lo que inyectan los controllers. Habla unicamente con casos de uso: no toca puertos de
 * salida ni sabe como se guarda un usuario ni como se firma un token.
 *
 * <p>Su trabajo es resolver la <em>ausencia</em>: lo que el caso de uso devuelve como
 * vacio se convierte aqui en una excepcion de dominio.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final GetUserUseCase getUserUseCase;

    public User register(RegistrationData data) {
        return registerUserUseCase.register(data);
    }

    public Session login(LoginCredentials credentials) {
        return loginUseCase.login(credentials);
    }

    public User getByLogin(String login) {
        return getUserUseCase.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));
    }
}
