package com.bartolomeo.clinics.booking.user.domain.model;

import java.time.Instant;

import lombok.*;

/**
 * Sesion iniciada: el token y el usuario al que pertenece.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    private String token;
    private User user;
    private Instant expiresAt;
}
