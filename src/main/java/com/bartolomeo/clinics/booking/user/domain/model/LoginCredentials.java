package com.bartolomeo.clinics.booking.user.domain.model;

import lombok.*;

/**
 * Credenciales con las que alguien inicia sesion.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentials {

    private String login;
    private String password;
}
