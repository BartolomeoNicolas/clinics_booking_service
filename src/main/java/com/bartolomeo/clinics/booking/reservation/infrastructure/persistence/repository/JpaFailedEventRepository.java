package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.FailedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de eventos irrecuperables.
 */
public interface JpaFailedEventRepository extends JpaRepository<FailedEventEntity, Long> {
}
