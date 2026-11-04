package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.adapter;

import com.bartolomeo.clinics.booking.reservation.domain.ports.out.FailedEventRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.FailedEventEntity;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository.JpaFailedEventRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementa el port de salida de eventos irrecuperables sobre Spring Data.
 *
 * <p><strong>El mismo evento no se registra dos veces.</strong> La columna del identificador
 * es única, así que un mensaje que falla dos veces —porque se reintentó o porque llegó
 * repetido— queda anotado una sola vez: el registro dice que el evento no se pudo aplicar,
 * no cuántas veces se intentó.
 */
@Component
@RequiredArgsConstructor
public class JpaFailedEventAdapter implements FailedEventRepository {

    private final JpaFailedEventRepository jpaFailedEventRepository;
    private final Clock clock;

    @Override
    public void save(String eventId, String eventType, String errorMessage) {
        FailedEventEntity entity = FailedEventEntity.builder()
                .eventId(eventId)
                .eventType(eventType)
                .errorMessage(acortar(errorMessage))
                .failedAt(Instant.now(clock))
                .build();
        jpaFailedEventRepository.save(entity);
    }

    /** La columna es corta y el error puede ser largo: se corta y no se pierde el evento. */
    private String acortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() <= 500 ? mensaje : mensaje.substring(0, 497) + "...";
    }
}
