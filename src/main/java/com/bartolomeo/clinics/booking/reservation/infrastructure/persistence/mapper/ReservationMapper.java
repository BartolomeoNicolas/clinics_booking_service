package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.mapper;

import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.stereotype.Component;

/**
 * De reserva de dominio a fila y de fila a reserva.
 */
@Component
public class ReservationMapper {

    /**
     * @param reservation reserva de dominio
     * @return la entidad que se persiste, o {@code null} si la reserva es nula
     */
    public ReservationEntity toEntity(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        return ReservationEntity.builder()
                .id(reservation.id())
                .catedraReservationId(reservation.catedraReservationId())
                .processId(reservation.processId())
                .userId(reservation.userId())
                .professionalId(reservation.professionalId())
                .appointmentDate(reservation.date())
                .startTime(reservation.startTime())
                .endTime(reservation.endTime())
                .status(reservation.status())
                .professionalFirstName(reservation.professionalFirstName())
                .professionalLastName(reservation.professionalLastName())
                .patientFirstName(reservation.patientFirstName())
                .patientLastName(reservation.patientLastName())
                .createdAt(reservation.createdAt())
                .build();
    }

    /**
     * @param entity fila persistida
     * @return la reserva de dominio, o {@code null} si la entidad es nula
     */
    public Reservation toDomain(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Reservation(
                entity.getId(),
                entity.getCatedraReservationId() == null ? 0L : entity.getCatedraReservationId(),
                entity.getProcessId() == null ? 0L : entity.getProcessId(),
                entity.getUserId() == null ? 0L : entity.getUserId(),
                entity.getProfessionalId() == null ? 0L : entity.getProfessionalId(),
                entity.getAppointmentDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getProfessionalFirstName(),
                entity.getProfessionalLastName(),
                entity.getPatientFirstName(),
                entity.getPatientLastName(),
                entity.getCreatedAt());
    }
}
