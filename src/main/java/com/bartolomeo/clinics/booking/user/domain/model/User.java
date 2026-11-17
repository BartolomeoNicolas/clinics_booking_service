package com.bartolomeo.clinics.booking.user.domain.model;

import java.util.List;

import lombok.*;

/**
 * Usuario final de la aplicacion.
 *
 * <p>Modelo de dominio puro: sin Spring, sin JPA, sin anotaciones de persistencia. Los
 * campos que el enunciado dice que administra el backend y no puede elegir quien se registra
 * (id, autoridades, auditoria) estan aqui, pero solo el backend los setea.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String login;
    /**
     * Hash de la contrasena, nunca la contrasena.
     *
     * <p>Vive en el modelo porque el repositorio guarda un {@code User}. No se expone nunca
     * en una respuesta: el DTO del usuario no tiene este campo. Y como la clase usa las
     * anotaciones de Lombok explicitas y no {@code @Data}, no se genera un {@code toString}
     * que pudiera imprimirlo.
     */
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
    private boolean activated;
    private List<Role> roles;
}
