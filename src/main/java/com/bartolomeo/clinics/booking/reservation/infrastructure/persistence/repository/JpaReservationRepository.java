package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de reservas confirmadas.
 */
public interface JpaReservationRepository extends JpaRepository<ReservationEntity, Long> {

    /**
     * La reserva de un proceso.
     *
     * @param processId proceso local
     * @return la reserva, si existe
     */
    Optional<ReservationEntity> findByProcessId(Long processId);

    /**
     * La reserva por el identificador de la cátedra.
     *
     * @param catedraReservationId identificador de la reserva en la cátedra
     * @return la reserva, si existe
     */
    Optional<ReservationEntity> findByCatedraReservationId(Long catedraReservationId);
}
