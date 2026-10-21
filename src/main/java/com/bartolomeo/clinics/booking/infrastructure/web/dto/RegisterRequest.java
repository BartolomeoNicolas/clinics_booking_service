package com.bartolomeo.clinics.booking.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Peticion de registro.
 *
 * <p>Los limites siguen los de la catedra: login de 3 a 50, contrasena de 4 a 100, email
 * valido de 5 a 254, langKey de 2 a 10. La imagen es opcional.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El login es obligatorio")
    @Size(min = 3, max = 50, message = "El login debe tener entre 3 y 50 caracteres")
    private String login;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 4, max = 100, message = "La contrasena debe tener entre 4 y 100 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede pasar de 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede pasar de 50 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(min = 5, max = 254, message = "El email debe tener entre 5 y 254 caracteres")
    private String email;

    @Size(max = 255, message = "La URL de imagen no puede pasar de 255 caracteres")
    private String imageUrl;

    @Size(min = 2, max = 10, message = "El idioma debe tener entre 2 y 10 caracteres")
    private String langKey;
}
