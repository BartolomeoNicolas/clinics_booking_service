package com.bartolomeo.clinics.booking.user.domain.ports.out;

/**
 * Hasheo y verificacion de contrasenas.
 *
 * <p>El dominio no sabe que existe BCrypt: por eso el hasheo esta detras de un port. La
 * implementacion vive en infrastructure/security.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
