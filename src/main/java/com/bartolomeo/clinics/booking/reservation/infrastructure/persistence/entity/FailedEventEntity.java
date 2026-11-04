package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA de los eventos que no se pudieron aplicar.
 *
 * <p><strong>Es un registro de diagnóstico.</strong> No se reintentan solos y no salen por
 * ninguna API: quedan para poder mirarlos cuando algo no cuadra. Por eso el mensaje de
 * error se guarda en texto y no solo el identificador del evento.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "failed_event")
public class FailedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 128, unique = true)
    private String eventId;

    @Column(name = "event_type", length = 48)
    private String eventType;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;
}
