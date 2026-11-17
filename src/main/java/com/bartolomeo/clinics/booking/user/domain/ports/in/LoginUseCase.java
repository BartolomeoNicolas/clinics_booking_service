package com.bartolomeo.clinics.booking.user.domain.ports.in;

import com.bartolomeo.clinics.booking.user.domain.model.LoginCredentials;
import com.bartolomeo.clinics.booking.user.domain.model.Session;

/**
 * Inicia sesion y emite el token.
 */
public interface LoginUseCase {

    /**
     * @return la sesion con el token de sesion y el usuario.
     * @throws com.bartolomeo.clinics.booking.user.application.exception.InvalidCredentialsException
     *         si el login no existe o la contrasena no coincide.
     */
    Session login(LoginCredentials credentials);
}
