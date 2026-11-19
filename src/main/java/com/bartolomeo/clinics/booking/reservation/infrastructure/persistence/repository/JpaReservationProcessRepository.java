package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    /**
     * El proceso por el identificador que le dio la cátedra.
     *
     * @param catedraProcessId proceso de la cátedra
     * @return el proceso, si existe
     */
    Optional<ReservationProcessEntity> findByCatedraProcessId(String catedraProcessId);

    /**
     * Los procesos que no están en un estado cerrado.
     *
     * @param estadosCerrados los estados sin salida
     * @return los procesos abiertos
     */
    List<ReservationProcessEntity> findByStatusNotIn(List<ReservationState> estadosCerrados);

    /**
     * Los procesos que la catedra marco como invalidos.
     *
     * @param estado el estado invalido
     * @return los procesos invalidos
     */
    List<ReservationProcessEntity> findByStatus(ReservationState estado);

    /**
     * Los procesos de un usuario, de los más nuevos a los más viejos.
     *
     * @param userId   usuario dueño de los procesos
     * @param pageable la página pedida
     * @return los procesos de ese usuario
     */
    List<ReservationProcessEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Cuántos procesos tiene un usuario.
     *
     * @param userId usuario dueño de los procesos
     * @return el total
     */
    long countByUserId(Long userId);
}
