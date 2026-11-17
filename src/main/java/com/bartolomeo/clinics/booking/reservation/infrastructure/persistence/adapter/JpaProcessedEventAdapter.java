package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.adapter;

import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ProcessedEventRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementa el port de salida de eventos procesados sobre Spring Data.
 */
@Component
@RequiredArgsConstructor
public class JpaProcessedEventAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaProcessedEventRepository;
    private final Clock clock;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaProcessedEventRepository.existsById(eventId);
    }

    @Override
    public void save(String eventId, int schemaVersion) {
        ProcessedEventEntity entity = ProcessedEventEntity.builder()
                .eventId(eventId)
                .schemaVersion(schemaVersion)
                .processedAt(Instant.now(clock))
                .build();
        jpaProcessedEventRepository.save(entity);
    }
}
