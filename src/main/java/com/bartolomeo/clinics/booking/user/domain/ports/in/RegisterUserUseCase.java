package com.bartolomeo.clinics.booking.user.domain.ports.in;

import com.bartolomeo.clinics.booking.user.domain.model.RegistrationData;
import com.bartolomeo.clinics.booking.user.domain.model.User;

/**
 * Registra un usuario final.
 *
 * <p>Un caso de uso por operacion. La interfaz vive en el dominio: es una afirmacion de lo que
 * el negocio puede hacer, no un detalle de la capa de aplicacion.
 */
public interface RegisterUserUseCase {

    /**
     * @return el usuario creado, ya activo.
     * @throws com.bartolomeo.clinics.booking.user.application.exception.LoginAlreadyTakenException
     *         si el login ya existe.
     * @throws com.bartolomeo.clinics.booking.user.application.exception.EmailAlreadyTakenException
     *         si el email ya existe.
     */
    User register(RegistrationData data);
}
