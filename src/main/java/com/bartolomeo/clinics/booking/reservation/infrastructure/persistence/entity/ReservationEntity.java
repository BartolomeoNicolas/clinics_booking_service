package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entidad JPA de la reserva confirmada.
 *
 * <p><strong>Los nombres son historia.</strong> Los del profesional y del paciente se
 * guardan como estaban cuando la cátedra confirmó, y no se usan para nada que no sea
 * mostrar la reserva: el catálogo vigente lo tiene el servicio de catálogo, y una copia acá
 * sería justo lo que el enunciado prohíbe.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservation")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** El identificador que le dio la cátedra. */
    @Column(name = "catedra_reservation_id")
    private Long catedraReservationId;

    /** El proceso local que la originó; único, porque el proceso manda. */
    @Column(name = "process_id", nullable = false, unique = true)
    private Long processId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** Datos históricos: lo que decía la cátedra en ese momento, no el catálogo. */
    @Column(name = "professional_first_name", length = 100)
    private String professionalFirstName;

    @Column(name = "professional_last_name", length = 100)
    private String professionalLastName;

    @Column(name = "patient_first_name", length = 100)
    private String patientFirstName;

    @Column(name = "patient_last_name", length = 100)
    private String patientLastName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
