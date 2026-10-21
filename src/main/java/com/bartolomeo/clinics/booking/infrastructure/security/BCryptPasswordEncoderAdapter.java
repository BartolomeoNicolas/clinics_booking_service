package com.bartolomeo.clinics.booking.infrastructure.security;

import com.bartolomeo.clinics.booking.user.domain.ports.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashea contrasenas con BCrypt.
 *
 * <p>BCrypt es el algoritmo por defecto de JHipster, y el enunciado pide que el modelo de
 * usuario sea compatible con el suyo. El costo por defecto (10) es el de JHipster.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
