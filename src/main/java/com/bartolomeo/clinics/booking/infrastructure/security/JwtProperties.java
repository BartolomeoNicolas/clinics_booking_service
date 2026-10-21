package com.bartolomeo.clinics.booking.infrastructure.security;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion del par RSA con el que este servicio firma los JWT de sesion.
 *
 * <p>Este servicio es el <strong>emisor</strong>: la clave privada vive aca y no sale nunca.
 * El servicio de catalogo solo necesita la publica, que se lee del mismo directorio de
 * secretos.
 *
 * <p>La clave se genera sola en el primer arranque si no existe, y se puede rotar con
 * {@code CLINICS_JWT_ROTATE=true}.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "clinics.security.jwt")
public class JwtProperties {

    /** Ruta del PEM con la clave privada. Fuera del repo, permisos 600. */
    @NotBlank(message = "falta la ruta de la clave privada: definí CLINICS_JWT_PRIVATE_KEY_PATH")
    private String privateKeyPath;

    /** Ruta del PEM con la clave publica. La escribe este servicio y la lee el de catalogo. */
    @NotBlank(message = "falta la ruta de la clave publica: definí CLINICS_JWT_PUBLIC_KEY_PATH")
    private String publicKeyPath;

    /** Audiencia de los tokens de sesion. La misma para los dos servicios. */
    @NotBlank(message = "falta la audiencia del token: definí CLINICS_JWT_AUDIENCE")
    private String audience;

    /** Emisor de los tokens de sesion. */
    private String issuer;

    /** Cuando vale {@code true}, el par se regenera en el arranque. */
    private boolean rotate;

    /**
     * Origenes permitidos por CORS.
     *
     * <p>Lista explicita, nunca comodin: cualquier cliente web que se agregue tiene que
     * estar declarado aca.
     */
    private List<String> allowedOrigins = List.of();

    /**
     * Tamano del par RSA en bits.
     *
     * <p>2048 es el minimo razonable hoy. Subirlo a 4096 hace el arranque mas lento sin
     * ganancia practica para tokens de sesion.
     */
    private int keySize = 2048;
}
