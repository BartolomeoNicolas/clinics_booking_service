package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.adapter;

import com.bartolomeo.clinics.booking.reservation.application.exception.DuplicateReservationRequestException;
import com.bartolomeo.clinics.booking.reservation.application.exception.ReservationPersistenceException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Implementa el port de salida de procesos sobre Spring Data.
 *
 * <p>Es el único lugar del servicio que conoce a la vez la interfaz de Spring Data y el port
 * del dominio. El adaptador es un {@code @Component}, no un {@code @Repository}.
 *
 * <p><strong>La violación de integridad se traduce.</strong> El índice único de
 * {@code (usuario, clave de idempotencia)} es la garantía de que un reintento no cree dos
 * procesos, y cuando choca con él la excepción que sale de la base no puede viajar al caso de
 * uso tal cual: se traduce al idioma del dominio, que sabe lo que significa "ya hay un
 * proceso para esta solicitud".
 */
@Component
@RequiredArgsConstructor
public class JpaReservationProcessAdapter implements ReservationProcessRepository {

    private final JpaReservationProcessRepository jpaReservationProcessRepository;
    private final ReservationProcessMapper mapper;

    @Override
    public Optional<ReservationProcess> findByRequestKey(long userId, String requestKey) {
        return jpaReservationProcessRepository.findByUserIdAndRequestKey(userId, requestKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ReservationProcess> findById(long id) {
        return jpaReservationProcessRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public ReservationProcess save(ReservationProcess process) {
        ReservationProcessEntity entity = mapper.toEntity(process);
        try {
            return mapper.toDomain(jpaReservationProcessRepository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            if (jpaReservationProcessRepository
                    .findByUserIdAndRequestKey(process.userId(), process.requestKey())
                    .isPresent()) {
                throw new DuplicateReservationRequestException(
                        process.userId(), process.requestKey());
            }
            // No es el índice de idempotencia: es otra restricción y no sabemos cuál.
            // Decir "duplicado" cuando no lo es sería mandarle al caso de uso una pista
            // falsa, así que se dice lo que se sabe: la escritura no se pudo hacer.
            throw new ReservationPersistenceException(
                    "No se pudo guardar el proceso de reserva.", ex);
        }
    }
}
