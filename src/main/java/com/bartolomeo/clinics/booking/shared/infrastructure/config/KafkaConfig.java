package com.bartolomeo.clinics.booking.shared.infrastructure.config;

import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraKafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cómo se consume Kafka en este servicio.
 *
 * <p><strong>La confirmación del offset es manual.</strong> El contenedor no confirma solo:
 * el listener confirma, y lo hace después de persistir. Es la diferencia entre "el mensaje
 * se procesó" y "el mensaje no se vuelve a entregar", y en un sistema donde el efecto es
 * irreversible —una reserva confirmada— la segunda es la que importa.
 *
 * <p><strong>Los reintentos están acotados.</strong> El contenedor reintenta con una espera
 * fija y un número máximo de intentos, y después de eso el mensaje se da por perdido. Un
 * reintento sin tope deja la partición trabada para los mensajes que sí se podían aplicar,
 * que es un costo mucho más caro que el de perder uno.
 *
 * <p><strong>La espera es corta y fija.</strong> No hace falta un backoff exponencial para
 * un consumidor que espera a la cátedra: lo que falla es infraestructura propia, y el
 * tiempo que tarda en volver a estar no lo conocemos.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            catedraKafkaListenerContainerFactory(
                    ConsumerFactory<String, String> consumerFactory,
                    CatedraKafkaProperties properties) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(properties.getRetryIntervalMs(), properties.getRetries())));
        return factory;
    }
}
