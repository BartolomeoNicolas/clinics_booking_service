package com.bartolomeo.clinics.booking.reservation.infrastructure.messaging;

import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraKafkaProperties;
import com.bartolomeo.clinics.booking.reservation.application.exception.PhoneSubmissionFailedException;
import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneSubmission;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.dto.AdditionalInformationSubmittedMessage;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica el teléfono en el topic de la cátedra.
 *
 * <p><strong>La clave del mensaje es el proceso.</strong> El contrato lo pide para que los
 * eventos de un mismo proceso conserven el orden dentro de la partición, y el orden importa:
 * la respuesta con el teléfono tiene que llegar después de la solicitud, no antes.
 *
 * <p><strong>Se espera a que el broker acepte el mensaje.</strong> Mandar en asíncrono y
 * seguir de largo dejaría un proceso diciendo "el teléfono salió" cuando el mensaje puede
 * haberse perdido. Aquí se espera, y si el broker no contesta se avisa: el proceso no se
 * guarda como enviado y el usuario puede reintentar.
 */
@Component
@RequiredArgsConstructor
public class PhoneSubmissionPublisherAdapter implements PhoneSubmissionPublisher {

    private static final Logger log = LoggerFactory.getLogger(PhoneSubmissionPublisherAdapter.class);

    private static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    private static final int SCHEMA_VERSION = 1;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CatedraKafkaProperties properties;
    private final Clock clock;

    @Override
    public void publish(PhoneSubmission submission) {
        AdditionalInformationSubmittedMessage mensaje =
                new AdditionalInformationSubmittedMessage(
                        submission.eventId(),
                        EVENT_TYPE,
                        properties.getGroupId(),
                        submission.reservationProcessId(),
                        submission.holdId(),
                        submission.requestEventId(),
                        new AdditionalInformationSubmittedMessage.Information(
                                submission.phoneNumber()),
                        Instant.now(clock),
                        SCHEMA_VERSION);

        String cuerpo;
        try {
            cuerpo = objectMapper.writeValueAsString(mensaje);
        } catch (RuntimeException ex) {
            throw new PhoneSubmissionFailedException(
                    "No se pudo armar el mensaje con el teléfono.", ex);
        }

        try {
            kafkaTemplate.send(
                            properties.getPhoneTopic(),
                            submission.reservationProcessId(),
                            cuerpo)
                    .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("Teléfono publicado para el proceso {}", submission.reservationProcessId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PhoneSubmissionFailedException(
                    "Se interrumpió el envío del teléfono a la cátedra.", ex);
        } catch (Exception ex) {
            throw new PhoneSubmissionFailedException(
                    "No se pudo enviar el teléfono a la cátedra: " + ex.getMessage(), ex);
        }
    }
}
