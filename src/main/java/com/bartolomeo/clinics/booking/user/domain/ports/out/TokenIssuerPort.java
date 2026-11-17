package com.bartolomeo.clinics.booking.user.domain.ports.out;

import com.bartolomeo.clinics.booking.user.domain.model.User;

import java.time.Instant;

/**
 * Emision del token de sesion.
 *
 * <p>El dominio no sabe que existe JWT ni Nimbus: la firma esta detras de un port.
 */
public interface TokenIssuerPort {

    /**
     * @return el token firmado y su instante de expiracion.
     */
    IssuedToken issue(User user);

    /**
     * Token emitido y cuando deja de ser valido.
     */
    record IssuedToken(String token, Instant expiresAt) {
    }
}
