package com.bartolomeo.clinics.booking.reservation.application.service;

import com.bartolomeo.clinics.booking.reservation.domain.model.Reservation;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Guarda la reserva que corresponde a un proceso.
 *
 * <p><strong>El proceso manda y la reserva es su consecuencia.</strong> Una reserva
 * confirmada no es un estado más del proceso: es el turno tomado de verdad. Por eso el
 * identificador local del proceso es la clave de la fila y hay una sola por proceso.
 *
 * <p><strong>Guardar dos veces no crea dos reservas.</strong> Un evento de confirmación que se
 * procesa dos veces, o una reconciliación que encuentra la reserva que el evento ya había
 * guardado, dejan la misma fila: el proceso manda y una reserva por proceso.
 *
 * <p><strong>Los datos descriptivos los completa la reconciliación.</strong> acá se guardan
 * el identificador de la cátedra y el turno, que es lo que se sabe sin preguntar nada; los
 * nombres del profesional y del paciente son los que la cátedra tenía en ese momento y se
 * escriben cuando se pregunta por REST. Son historia, no el catálogo vigente.
 */
@Component
@RequiredArgsConstructor
public class ReservationRegistrar {

    private static final String CONFIRMADA = "CONFIRMED";
    private static final String CANCELADA = "CANCELLED";

    private final ReservationRepository reservations;
    private final Clock clock;

    /**
     * Guarda la reserva confirmada del proceso.
     *
     * @param proceso              proceso confirmado
     * @param catedraReservationId identificador de la reserva en la cátedra; puede no venir
     */
    public void registraConfirmacion(ReservationProcess proceso, Long catedraReservationId) {
        if (reservations.findByProcessId(proceso.id()).isPresent()) {
            return;
        }

        reservations.save(new Reservation(
                null,
                catedraReservationId == null ? 0L : catedraReservationId,
                proceso.id(),
                proceso.userId(),
                proceso.professionalId(),
                proceso.date(),
                proceso.startTime(),
                null,
                CONFIRMADA,
                null,
                null,
                null,
                null,
                Instant.now(clock)));
    }

    /**
     * Cancela la reserva del proceso, si la había.
     *
     * @param proceso proceso cancelado
     */
    public void registraCancelacion(ReservationProcess proceso) {
        Optional<Reservation> existente = reservations.findByProcessId(proceso.id());
        if (existente.isEmpty()) {
            return;
        }
        reservations.save(existente.get().conEstado(CANCELADA));
    }
}
