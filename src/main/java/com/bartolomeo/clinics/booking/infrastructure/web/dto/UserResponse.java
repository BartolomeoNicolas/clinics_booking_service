package com.bartolomeo.clinics.booking.infrastructure.web.dto;

import java.util.List;

import lombok.*;

/**
 * Respuesta con los datos de un usuario.
 *
 * <p>No tiene hash de contrasena: ese dato existe en el dominio pero nunca sale por HTTP.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
    private boolean activated;
    private List<String> authorities;
}
