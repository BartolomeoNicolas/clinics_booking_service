package com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.mapper;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidCatedraEventException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.dto.CatedraEventEnvelope;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Del mensaje de Kafka al idioma del dominio.
 *
 * <p><strong>El vocabulario de la cátedra se traduce acá y en ningún otro lado.</strong> Los
 * nombres de los eventos son de la cátedra; el dominio habla de hechos del negocio. Saber
 * que {@code AppointmentConfirmed} quiere decir "la reserva quedó confirmada" es conocimiento
 * del contrato, y el contrato es de la cátedra.
 *
 * <p><strong>Lo que no se puede entender se rechaza acá.</strong> Un JSON que no se puede
 * parsear, un evento sin identificador, una versión del contrato que no se conoce: nada de
 * eso se arregla reintentando, y por eso se dice de una vez y no se lo lleva al proceso.
 */
@Component
@RequiredArgsConstructor
public class CatedraEventMapper {

    /** La versión del contrato que este servicio entiende. */
    public static final int SCHEMA_VERSION = 1;

    /** Qué evento de la cátedra es cada hecho del negocio. */
    private static final Map<String, ReservationEvent> EVENTOS = Map.of(
            "AdditionalInformationRequested", ReservationEvent.INITIAL_CONFIRMATION,
            "AdditionalInformationRejected", ReservationEvent.PHONE_REJECTED,
            "AppointmentConfirmed", ReservationEvent.CONFIRMED,
            "AppointmentCancelled", ReservationEvent.CANCELLED,
            "AppointmentProcessExpired", ReservationEvent.EXPIRED,
            "AppointmentProcessInvalid", ReservationEvent.INVALID);

    private final ObjectMapper objectMapper;

    /**
     * Lee y valida el mensaje.
     *
     * @param json cuerpo del mensaje
     * @return el sobre, ya validado
     * @throws InvalidCatedraEventException si el mensaje no se puede entender
     */
    public CatedraEventEnvelope parse(String json) {
        CatedraEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(json, CatedraEventEnvelope.class);
        } catch (RuntimeException ex) {
            throw new InvalidCatedraEventException(
                    "El mensaje de la cátedra no se pudo leer: " + ex.getMessage());
        }

        if (envelope == null) {
            throw new InvalidCatedraEventException("El mensaje de la cátedra vino vacío.");
        }
        if (envelope.eventId() == null || envelope.eventId().isBlank()) {
            throw new InvalidCatedraEventException(
                    "El mensaje de la cátedra no tiene eventId: sin él no hay idempotencia.");
        }
        if (envelope.reservationProcessId() == null || envelope.reservationProcessId().isBlank()) {
            throw new InvalidCatedraEventException(
                    "El mensaje de la cátedra no dice de qué proceso es.");
        }
        if (envelope.schemaVersion() == null || envelope.schemaVersion() != SCHEMA_VERSION) {
            throw new InvalidCatedraEventException(
                    "El mensaje de la cátedra trae la versión de contrato "
                            + envelope.schemaVersion() + " y este servicio entiende la "
                            + SCHEMA_VERSION + ".");
        }
        return envelope;
    }

    /**
     * El hecho del negocio que corresponde al tipo de evento de la cátedra.
     *
     * @param eventType tipo del evento, en el vocabulario de la cátedra
     * @return el hecho, o vacío si el tipo no se conoce
     */
    public Optional<ReservationEvent> toDomainEvent(String eventType) {
        return Optional.ofNullable(EVENTOS.get(eventType));
    }

    /**
     * El tipo de evento de la cátedra, o un texto que lo identifique aunque no se conozca.
     *
     * @param envelope sobre del mensaje
     * @return el tipo, o {@code null} si el mensaje ni siquiera se pudo leer
     */
    public String typeOf(CatedraEventEnvelope envelope) {
        return envelope == null ? null : envelope.eventType();
    }
}
