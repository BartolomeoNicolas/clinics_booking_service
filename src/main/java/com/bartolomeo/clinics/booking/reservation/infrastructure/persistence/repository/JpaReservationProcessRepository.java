package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de procesos de reserva.
 *
 * <p>Es la interfaz de Spring Data; el dominio no la ve. El adaptador
 * {@code JpaReservationProcessAdapter} es el que la traduce a entidades y a procesos.
 */
public interface JpaReservationProcessRepository
        extends JpaRepository<ReservationProcessEntity, Long> {

    /**
     * El proceso de un usuario con esa clave de solicitud.
     *
     * @param userId     usuario que inició la reserva
     * @param requestKey clave de idempotencia
     * @return el proceso, si existe
     */
    Optional<ReservationProcessEntity> findByUserIdAndRequestKey(Long userId, String requestKey);
}
