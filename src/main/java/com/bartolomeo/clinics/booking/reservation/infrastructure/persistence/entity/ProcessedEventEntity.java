package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA de los eventos ya procesados.
 *
 * <p><strong>La clave primaria es el {@code eventId}.</strong> No hay un id propio: el
 * identificador del evento es lo que lo hace único, y esa unicidad es la que impide que el
 * mismo evento se aplique dos veces. Por eso la tabla es también la que garantiza la
 * idempotencia, y no solo la que la registra.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
