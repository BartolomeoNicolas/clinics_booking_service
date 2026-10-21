package com.bartolomeo.clinics.booking.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Secretos que el servicio necesita del entorno para arrancar.
 *
 * <p>Viven aca, y no en {@code application.yml}, porque son valores sensibles y porque
 * queremos que su ausencia rompa el arranque temprano y con un mensaje que diga que falta.
 *
 * <p>Un placeholder pelado como {@code ${DB_PASSWORD}} <em>no</em> falla cuando no esta
 * definido: resuelve a vacio y el arranque sigue hasta que algo intenta autenticarse, que es
 * mucho mas tarde y con un error que no menciona la variable. Con {@code @Validated} y
 * {@code @NotBlank} el fallo ocurre al enlazar las propiedades, y Spring nombra la propiedad
 * exacta.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "clinics.local")
public class LocalProperties {

    /**
     * Contrasena de la base de datos local. Requerida.
     *
     * <p>Se inyecta en {@code spring.datasource.password} a traves de
     * {@code ${clinics.local.db-password}}.
     */
    @NotBlank(message = "falta la contrasena de la base de datos: definí DB_PASSWORD en .env o en ~/.config/clinics/secrets/booking.yml")
    private String dbPassword;
}
