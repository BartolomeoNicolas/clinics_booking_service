package com.bartolomeo.clinics.booking.catalog.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración del cliente del servicio de catálogo.
 *
 * <p>Son cuatro datos y ninguno es un secreto: dónde está el servicio, cómo se llama el que
 * llama, y cuánto se le tiene paciencia. La clave con la que se firma lo que se le manda vive
 * en {@code shared.infrastructure.security}, porque es la misma que firma las sesiones de
 * este servicio.
 *
 * <p>{@code baseUrl} es obligatorio a propósito. Si falta, la aplicación no arranca, en vez de
 * arrancar y fallar en la primera consulta al catálogo con un error de URL vacía que no dice
 * nada.
 *
 * <p>El valor por defecto de {@code serviceName} es el de este servicio; se puede pisar desde
 * afuera para que dos instancias se presenten distinto, pero no hace falta cambiar código.
 *
 * <p>Los timeouts están en milisegundos para poder escribirse en el {@code application.yml}
 * como enteros legibles. Un catálogo caído no debería dejar a turnos colgado: son dos segundos
 * de conexión y cinco de espera, y al vencer se traduce en
 * {@link com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException}
 * con su causa original adjunta.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "clinics.catalog")
public class CatalogProperties {

    /** URL base del servicio de catálogo, sin barra final. */
    @NotBlank(message = "clinics.catalog.base-url es obligatorio")
    private String baseUrl;

    /** Identidad de este servicio dentro del JWT técnico que se le manda. */
    private String serviceName = "clinics-booking";

    /** Timeout de conexión en milisegundos. */
    @Min(value = 1, message = "clinics.catalog.connect-timeout-ms debe ser mayor que cero")
    private int connectTimeoutMs = 2_000;

    /** Timeout de lectura en milisegundos. */
    @Min(value = 1, message = "clinics.catalog.read-timeout-ms debe ser mayor que cero")
    private int readTimeoutMs = 5_000;
}
