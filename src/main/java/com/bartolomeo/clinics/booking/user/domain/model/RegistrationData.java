package com.bartolomeo.clinics.booking.user.domain.model;

import lombok.*;

/**
 * Datos con los que alguien se registra.
 *
 * <p>La contrasena viaja en claro desde el borde hasta el caso de uso, y ahi se hashea. No se
 * guarda, no se registra y no vuelve a salir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationData {

    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
}
