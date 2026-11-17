package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de eventos ya procesados.
 */
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
