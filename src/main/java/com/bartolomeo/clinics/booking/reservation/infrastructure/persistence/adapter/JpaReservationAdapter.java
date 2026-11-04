package com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.adapter;

import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.mapper.ReservationMapper;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository.JpaReservationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementa el port de salida de reservas sobre Spring Data.
 */
@Component
@RequiredArgsConstructor
public class JpaReservationAdapter implements ReservationRepository {

    private final JpaReservationRepository jpaReservationRepository;
    private final ReservationMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {
        return mapper.toDomain(jpaReservationRepository.save(mapper.toEntity(reservation)));
    }

    @Override
    public Optional<Reservation> findByProcessId(long processId) {
        return jpaReservationRepository.findByProcessId(processId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByCatedraReservationId(long catedraReservationId) {
        return jpaReservationRepository.findByCatedraReservationId(catedraReservationId)
                .map(mapper::toDomain);
    }
}
