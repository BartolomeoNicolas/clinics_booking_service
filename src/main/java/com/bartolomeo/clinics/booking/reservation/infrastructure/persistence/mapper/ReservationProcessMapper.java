package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.mapper;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.stereotype.Component;

/**
 * De proceso de dominio a fila y de fila a proceso.
 *
 * <p>El estado se traduce en los dos sentidos, y es el único campo que necesita traducción:
 * en la base es texto y en el dominio es una enumeración, para que nadie tenga que comparar
 * cadenas ni preguntarse si el estado guardado es uno de los conocidos.
 */
@Component
public class ReservationProcessMapper {

    /**
     * @param process proceso de dominio
     * @return la entidad que se persiste, o {@code null} si el proceso es nulo
     */
    public ReservationProcessEntity toEntity(ReservationProcess process) {
        if (process == null) {
            return null;
        }
        return ReservationProcessEntity.builder()
                .id(process.id())
                .userId(process.userId())
                .externalPatientId(process.externalPatientId())
                .status(process.state())
                .professionalId(process.professionalId())
                .appointmentDate(process.date())
                .startTime(process.startTime())
                .holdId(process.holdId())
                .catedraProcessId(process.catedraProcessId())
                .requestEventId(process.requestEventId())
                .expiresAt(process.expiresAt())
                .requestKey(process.requestKey())
                .failureReason(process.failureReason())
                .createdAt(process.createdAt())
                .updatedAt(process.updatedAt())
                .build();
    }

    /**
     * @param entity fila persistida
     * @return el proceso de dominio, o {@code null} si la entidad es nula
     */
    public ReservationProcess toDomain(ReservationProcessEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ReservationProcess(
                entity.getId(),
                entity.getUserId(),
                entity.getExternalPatientId(),
                entity.getProfessionalId() == null ? 0L : entity.getProfessionalId(),
                entity.getAppointmentDate(),
                entity.getStartTime(),
                entity.getStatus(),
                entity.getHoldId(),
                entity.getCatedraProcessId(),
                entity.getRequestEventId(),
                entity.getExpiresAt(),
                entity.getRequestKey(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
