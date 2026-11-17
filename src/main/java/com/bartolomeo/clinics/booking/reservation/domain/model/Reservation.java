package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una reserva confirmada: el turno que quedó tomado de verdad.
 *
 * <p><strong>Es la foto de lo que dijo la cátedra en ese momento.</strong> Guarda el
 * identificador que le dio la cátedra, el turno, y los nombres del profesional y del
 * paciente tal como venían en ese momento. Son datos descriptivos: sirven para que la app
 * muestre "la reserva con la doctora Ana Gómez", no para saber quién atiende ni cuándo.
 *
 * <p><strong>Nada de esto es el catálogo vigente.</strong> El catálogo lo tiene el servicio de
 * catálogo, y si un profesional cambia de nombre o se da de baja, lo que diga esta reserva
 * de hace tres meses no lo corrige. Por eso estos datos se guardan como historia y no se
 * usan para mostrar disponibilidad ni para decidir si un turno existe. El enunciado (§4.2)
 * es tajante en que este servicio no guarda copia del catálogo, y una fila de reserva con
 * el nombre del profesional es exactamente eso si alguien la usa como fuente de verdad.
 *
 * @param id                    identificador local de la reserva
 * @param catedraReservationId  identificador de la reserva en la cátedra
 * @param processId             proceso local que la originó
 * @param userId                usuario que la inició
 * @param professionalId        profesional del turno
 * @param date                  día del turno
 * @param startTime             hora de inicio
 * @param endTime               hora de fin, si se conoce
 * @param status                estado de la reserva
 * @param professionalFirstName nombre del profesional en ese momento; es histórico
 * @param professionalLastName  apellido del profesional en ese momento; es histórico
 * @param patientFirstName      nombre del paciente en ese momento
 * @param patientLastName       apellido del paciente en ese momento
 * @param createdAt             cuándo se confirmó
 */
public record Reservation(
        Long id,
        long catedraReservationId,
        long processId,
        long userId,
        long professionalId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String professionalFirstName,
        String professionalLastName,
        String patientFirstName,
        String patientLastName,
        Instant createdAt) {

    /**
     * La misma reserva en otro estado.
     *
     * <p>La fecha de creación no cambia: es cuando se confirmó, y una cancelación posterior
     * no la mueve.
     *
     * @param status estado nuevo
     * @return la reserva con el estado nuevo
     */
    public Reservation conEstado(String status) {
        return new Reservation(
                id,
                catedraReservationId,
                processId,
                userId,
                professionalId,
                date,
                startTime,
                endTime,
                status,
                professionalFirstName,
                professionalLastName,
                patientFirstName,
                patientLastName,
                createdAt);
    }
}
