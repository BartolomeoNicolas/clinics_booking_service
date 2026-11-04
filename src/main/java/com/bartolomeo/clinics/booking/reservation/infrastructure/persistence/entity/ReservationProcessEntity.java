package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA del proceso de reserva.
 *
 * <p>Las anotaciones de persistencia viven acá y no en el modelo de dominio: el modelo no
 * depende de JPA.
 *
 * <p><strong>El estado se guarda como texto.</strong> El enum del dominio se persiste por
 * nombre, no por ordinal: si un día se agrega un estado en el medio, las filas que ya están
 * en la base tienen que seguir significando lo mismo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservation_process")
public class ReservationProcessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "external_patient_id", length = 100)
    private String externalPatientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReservationState status;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "hold_id", length = 64)
    private String holdId;

    /** El proceso de la cátedra, que es distinto del identificador local de la fila. */
    @Column(name = "catedra_process_id", length = 64)
    private String catedraProcessId;

    /** El evento de la cátedra que pidió el teléfono: es el que hay que responder. */
    @Column(name = "request_event_id", length = 128)
    private String requestEventId;

    /** El vencimiento que dio la cátedra; local no se recalcula ni se extiende. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Clave de idempotencia de la solicitud; única junto con el usuario. */
    @Column(name = "request_key", length = 128)
    private String requestKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
