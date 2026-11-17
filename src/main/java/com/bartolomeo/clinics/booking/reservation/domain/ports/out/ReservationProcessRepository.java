package com.bartolomeo.clinics.booking.reservation.domain.ports.out;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import java.util.Optional;

/**
 * Persistencia de los procesos de reserva.
 *
 * <p>Nombre de repositorio, no de puerto, como en el repo de referencia. Habla de procesos del
 * dominio: no conoce entidades ni JPA.
 *
 * <p><strong>La clave de idempotencia se busca, no se inventa.</strong> Es lo que permite
 * retomar un proceso que ya existe en vez de crear otro, y por eso el índice es único: dos
 * procesos del mismo usuario con la misma clave de solicitud no pueden existir, y si el
 * intento de crearlo choca con el índice, el adaptador lo dice con una excepción propia en
 * vez de dejar pasar el error de la base.
 */
public interface ReservationProcessRepository {

    /**
     * El proceso de un usuario con esa clave de solicitud, si lo hay.
     *
     * @param userId     usuario que inició la reserva
     * @param requestKey clave de idempotencia de la solicitud
     * @return el proceso existente, o vacío
     */
    Optional<ReservationProcess> findByRequestKey(long userId, String requestKey);

    /**
     * El proceso por su identificador local.
     *
     * @param id identificador local
     * @return el proceso, o vacío
     */
    Optional<ReservationProcess> findById(long id);

    /**
     * El proceso por el identificador que le dio la cátedra.
     *
     * <p><strong>Es el que aparece en los mensajes de Kafka.</strong> La cátedra no conoce
     * los identificadores locales, así que el proceso se busca por el suyo; el local es
     * para la app.
     *
     * @param catedraProcessId proceso de la cátedra
     * @return el proceso, o vacío si esta cátedra no es la dueña de ese proceso
     */
    Optional<ReservationProcess> findByCatedraProcessId(String catedraProcessId);

    /**
     * Guarda el proceso.
     *
     * @param process proceso a guardar
     * @return el proceso guardado, con su identificador
     * @throws com.bartolomeo.clinics.booking.reservation.application.exception.DuplicateReservationRequestException
     *         si ya hay un proceso de ese usuario con la misma clave de solicitud
     */
    ReservationProcess save(ReservationProcess process);
}
