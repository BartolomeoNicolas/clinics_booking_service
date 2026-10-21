package com.bartolomeo.clinics.booking.infrastructure.web.dto;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de un inicio de sesion.
 *
 * <p>Los nombres de las propiedades JSON siguen el formato de JHipster para la respuesta
 * OAuth2 ({@code id_token}, {@code access_token}, {@code token_type}, {@code expires_in}) porque
 * el enunciado pide que el contrato de usuarios sea compatible con el de JHipster. El resto del
 * proyecto usa camelCase, como pide el contrato general.
 *
 * <p>Los campos Java quedan en camelCase y el nombre JSON se declara con
 * {@code @JsonProperty}: asi el codigo sigue la convencion del lenguaje y el cable sigue el de
 * JHipster.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    private String scope;

    private UserResponse user;
}
