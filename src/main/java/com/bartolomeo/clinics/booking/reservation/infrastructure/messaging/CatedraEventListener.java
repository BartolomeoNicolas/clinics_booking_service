package com.bartolomeo.clinics.booking.reservation.infrastructure.messaging;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidCatedraEventException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEventEnvelope;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.FailedEventRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.dto.CatedraEventEnvelope;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.mapper.CatedraEventMapper;
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Escucha los eventos de turnos de la cátedra.
 *
 * <p><strong>El offset se confirma después de persistir, nunca antes.</strong> Si se
 * confirmara antes, un corte entre la confirmación y el guardado haría que el mensaje se
 * perdiera para siempre: Kafka ya no lo mandaría y el proceso se quedaría a medio hacer sin
 * que nadie lo supiera. Por eso la confirmación manual es la última cosa que hace el
 * listener.
 *
 * <p><strong>Lo que no se puede arreglar se registra y se confirma.</strong> Un mensaje
 * ilegible, un tipo de evento que no se conoce, un evento de un proceso ajeno: ninguno de
 * esos casos mejora con otro intento, y dejarlo sin confirmar haría que el mismo mensaje
 * volviera a llegar para siempre y trabara la partición. Queda anotado en
 * {@code failed_event} para poder mirarlo.
 *
 * <p><strong>Lo que sí se puede arreglar no se pierde.</strong> Un error de infraestructura
 * —la base caída, un timeout— se deja pasar hacia el contenedor, que lo reintenta con un
 * tope. El offset no se confirma mientras tanto, así que el mensaje vuelve a llegar cuando
 * el problema se haya resuelto. Perderlo sería peor que reprocesarlo.
 *
 * <p><strong>El mensaje repetido no repite el efecto.</strong> La idempotencia está en el
 * caso de uso, que consulta el {@code eventId} ya procesado antes de tocar el proceso.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "clinics.catedra.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CatedraEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatedraEventListener.class);

    private final CatedraEventMapper mapper;
    private final ProcessReservationEventUseCase processEvent;
    private final FailedEventRepository failedEvents;

    /**
     * Recibe un evento de la cátedra.
     *
     * @param record         mensaje de Kafka, con su clave y su cuerpo
     * @param acknowledgment confirmación manual del offset
     */
    @KafkaListener(
            topics = "${clinics.catedra.kafka.actions-topic}",
            groupId = "${clinics.catedra.kafka.consumer-group}",
            containerFactory = "catedraKafkaListenerContainerFactory")
    public void onCatedraEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {

        CatedraEventEnvelope envelope;
        try {
            envelope = mapper.parse(record.value());
        } catch (InvalidCatedraEventException ex) {
            log.warn("Mensaje de la cátedra irrecuperable: {}", ex.getMessage());
            registrarFallido(record, null, ex.getMessage());
            acknowledgment.acknowledge();
            return;
        }

        Optional<ReservationEvent> evento = mapper.toDomainEvent(envelope.eventType());
        if (evento.isEmpty()) {
            String mensaje = "La cátedra mandó un evento de tipo desconocido: "
                    + envelope.eventType();
            log.warn("{}", mensaje);
            registrarFallido(record, envelope.eventType(), mensaje);
            acknowledgment.acknowledge();
            return;
        }

        ReservationEventEnvelope paraElDominio = new ReservationEventEnvelope(
                envelope.eventId(),
                evento.get(),
                envelope.reservationProcessId(),
                envelope.schemaVersion(),
                envelope.occurredAt(),
                envelope.reservationId(),
                envelope.reason());

        try {
            processEvent.process(paraElDominio);
        } catch (ApplicationException ex) {
            // Los errores propios son irrecuperables: son de negocio, no de infraestructura.
            log.warn("Evento {} irrecuperable: {}", envelope.eventId(), ex.getMessage());
            registrarFallido(record, envelope.eventType(), ex.getMessage());
            acknowledgment.acknowledge();
            return;
        } catch (RuntimeException ex) {
            // Esto sí se puede arreglar: se deja pasar hacia el contenedor, que reintenta
            // con un tope. El offset no se confirma, así que el mensaje vuelve a llegar.
            log.error("Falla recuperable al aplicar el evento {}: {}",
                    envelope.eventId(), ex.getMessage());
            throw ex;
        }

        acknowledgment.acknowledge();
    }

    /**
     * Anota el evento como irrecuperable.
     *
     * <p>El identificador y el tipo se sacan del mensaje best effort: si el mensaje no se
     * pudo leer, no hay identificador, y lo que queda es el texto del error.
     */
    private void registrarFallido(
            ConsumerRecord<String, String> record, String eventType, String mensaje) {

        String eventId = null;
        String tipo = eventType;
        try {
            CatedraEventEnvelope envelope = mapper.parse(record.value());
            eventId = envelope.eventId();
            tipo = envelope.eventType();
        } catch (RuntimeException ex) {
            // El mensaje no se pudo leer: se registra igual, con lo que se pueda.
            log.debug("No se pudo leer el mensaje fallido para sacarle el identificador");
        }

        failedEvents.save(eventId, tipo, mensaje);
    }
}
