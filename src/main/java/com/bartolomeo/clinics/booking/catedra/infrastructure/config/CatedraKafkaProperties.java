package com.bartolomeo.clinics.booking.catedra.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Los topics de Kafka de la cátedra.
 *
 * <p><strong>Los nombres los pone la cátedra y llevan nuestro {@code groupId}.</strong> El
 * topic de acciones es de la cátedra hacia nosotros y el del teléfono es nuestro hacia la
 * cátedra; los dos se llaman con el mismo grupo, que es como la cátedra separa lo de cada
 * alumno.
 *
 * <p><strong>El topic del teléfono también se configura acá, aunque todavía no se use.</strong>
 * Publicar en un topic que no existe no es un error visible hasta que el mensaje se pierde,
 * y ese topic lo crea la cátedra: tener el nombre a mano desde el principio evita que
 * alguien lo invente después.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "clinics.catedra.kafka")
public class CatedraKafkaProperties {

    /**
     * Grupo de la cuenta técnica.
     *
     * <p>Es el mismo {@code groupId} que aparece en los topics y que va en el mensaje que
     * este servicio publica: es cómo la cátedra sabe de qué cuenta es el mensaje.
     */
    @NotBlank(message = "falta el groupId de la cuenta tecnica: definí CATEDRA_GROUP_ID")
    private String groupId;

    /**
     * Topic donde la cátedra publica los eventos de turnos.
     *
     * <p>Es el que consume este servicio.
     */
    @NotBlank(message = "falta el topic de acciones de la cátedra: definí CATEDRA_ACTIONS_TOPIC")
    private String actionsTopic;

    /**
     * Topic donde este servicio publica la respuesta con el teléfono.
     *
     * <p>Es el que consume la cátedra.
     */
    @NotBlank(message = "falta el topic de teléfono de la cátedra: definí CATEDRA_PHONE_TOPIC")
    private String phoneTopic;

    /**
     * Grupo de consumidores de este servicio.
     *
     * <p>Tantos servicios como grupos distintos: si dos servicios compartieran el grupo, se
     * repartirían los mensajes entre los dos y cada uno recibiría la mitad.
     */
    @NotBlank(message = "falta el grupo de consumidores: definí CATEDRA_CONSUMER_GROUP")
    private String consumerGroup;

    /**
     * Cuántos reintentos se dan ante un error recuperable.
     *
     * <p>Acotados a propósito: un error que se repite no se arregla insistiendo, y un bucle
     * sin fin deja la partición trabada para los mensajes que sí se podían aplicar.
     */
    private int retries = 2;

    /**
     * Cuánto se espera entre reintentos, en milisegundos.
     *
     * <p>La espera se hace en el hilo del consumidor y no en el scheduler, para no salir del
     * orden de la partición: los eventos de un mismo proceso comparten clave de mensaje y
     * tienen que seguir ese orden.
     */
    private long retryIntervalMs = 500L;

    /**
     * Cuánto se espera a que el broker acepte un mensaje, en milisegundos.
     *
     * <p>Se espera de verdad: mandar en asíncrono y seguir de largo dejaría un proceso
     * diciendo "el teléfono salió" cuando el mensaje puede haberse perdido.
     */
    private long sendTimeoutMs = 5000L;
}
