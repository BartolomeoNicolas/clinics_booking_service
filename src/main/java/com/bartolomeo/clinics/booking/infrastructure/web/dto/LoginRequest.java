package com.bartolomeo.clinics.booking.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El login es obligatorio")
    @Size(min = 1, max = 254, message = "El login debe tener entre 1 y 254 caracteres")
    private String login;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 4, max = 100, message = "La contrasena debe tener entre 4 y 100 caracteres")
    private String password;
}
