package com.bartolomeo.clinics.booking.reservation.domain.model;

import com.bartolomeo.clinics.booking.reservation.domain.ReservationStateMachine;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Un proceso de reserva: el seguimiento local de lo que el usuario pidió y de lo que la cátedra
 * fue contestando.
 *
 * <p><strong>Es una fila, no una lista de cosas por hacer.</strong> Existe antes de la primera
 * llamada a la cátedra y existe para que un timeout no deje nada atrás: si la llamada se
 * pierde, queda un proceso con el identificador del turno y el usuario que lo pidió, que es
 * exactamente lo que hace falta para reconciliar (anexo §18.4). Por eso se guarda antes de
 * llamar y no después.
 *
 * <p><strong>Los identificadores de la cátedra se guardan, no se inventan.</strong> El
 * {@code holdId} y el {@code reservationProcessId} son los que la cátedra dio, y el
 * {@code expiresAt} es el suyo: la vigencia real la determina la cátedra y una copia local no
 * la extiende (enunciado §7). Guardar el vencimiento no es guardar una copia del retén, es
 * anotar cuándo deja de servir para no prometer más de lo que hay.
 *
 * <p><strong>El usuario va con el proceso.</strong> Es lo que hace que un usuario solo vea y
 * cancele los procesos propios (enunciado §9), y por eso el identificador del paciente se
 * deriva de él y no de lo que manda el cliente.
 *
 * @param id                 identificador local del proceso; es el handle que ve la app
 *                           porque existe antes que la cátedra
 * @param userId             usuario que lo inició
 * @param externalPatientId  identificador del paciente en la forma de la cátedra
 * @param professionalId     profesional del turno
 * @param date               día del turno
 * @param startTime          hora de inicio del turno
 * @param state              estado del proceso
 * @param holdId             retén de la cátedra; nulo hasta que lo crea
 * @param catedraProcessId   proceso de la cátedra; nulo hasta que lo crea
 * @param expiresAt          vencimiento, el de la cátedra; nulo hasta que lo sepa
 * @param requestKey         clave de idempotencia de la solicitud
 * @param failureReason      por qué falló, si falló
 * @param createdAt          cuándo se creó
 * @param updatedAt          cuándo se tocó por última vez
 */
public record ReservationProcess(
        Long id,
        Long userId,
        String externalPatientId,
        long professionalId,
        LocalDate date,
        LocalTime startTime,
        ReservationState state,
        String holdId,
        String catedraProcessId,
        Instant expiresAt,
        String requestKey,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Un proceso recién soliciting, todavía sin nada pedido a la cátedra.
     *
     * @param userId           usuario que lo inició
     * @param externalPatientId identificador del paciente en la forma de la cátedra
     * @param data             lo que pidió el usuario
     * @param now              instante de la creación
     */
    public static ReservationProcess initiated(
            Long userId, String externalPatientId, ReservationData data, Instant now) {

        return new ReservationProcess(
                null,
                userId,
                externalPatientId,
                data.professionalId(),
                data.date(),
                data.startTime(),
                ReservationState.INITIATED,
                null,
                null,
                null,
                data.requestKey(),
                null,
                now,
                now);
    }

    /**
     * Aplica un evento y devuelve el proceso en el estado nuevo.
     *
     * <p><strong>Devuelve vacío cuando el evento no aplica.</strong> El dominio no depende
     * de la capa de aplicación (R1), así que no puede lanzar la excepción del error: lo
     * devuelve vacío y es la capa de aplicación la que lo convierte en error. Así queda
     * clarísimo en el código qué es una regla de negocio y qué es un problema del pedido.
     *
     * @param event evento que ocurrió
     * @param now   instante del cambio
     * @return el proceso en el estado nuevo, o vacío si la transición no existe
     */
    public Optional<ReservationProcess> apply(ReservationEvent event, Instant now) {
        return apply(event, now, null);
    }

    /**
     * Aplica un evento, dejando escrito por qué si el evento es un fallo.
     *
     * @param event  evento que ocurrió
     * @param now    instante del cambio
     * @param reason motivo del fallo; solo se guarda si el evento es {@code FAILED}
     * @return el proceso en el estado nuevo, o vacío si la transición no existe
     */
    public Optional<ReservationProcess> apply(ReservationEvent event, Instant now, String reason) {
        return ReservationStateMachine.nextState(state, event)
                .map(nuevo -> copy(
                        nuevo,
                        holdId,
                        catedraProcessId,
                        expiresAt,
                        nuevo == ReservationState.FAILED ? reason : failureReason,
                        now));
    }

    /**
     * La cátedra tomó el turno: hay retén, con su identificador y su vencimiento.
     *
     * @param holdId           retén que creó la cátedra
     * @param catedraProcessId proceso que creó la cátedra
     * @param expiresAt        vencimiento que da la cátedra
     * @param now              instante del cambio
     * @return el proceso con el retén, o vacío si el estado actual no admitía este paso
     */
    public Optional<ReservationProcess> holdCreated(
            String holdId, String catedraProcessId, Instant expiresAt, Instant now) {

        return apply(ReservationEvent.HOLD_CREATED, now)
                .map(nuevo -> copy(
                        nuevo.state(),
                        holdId,
                        catedraProcessId,
                        expiresAt,
                        nuevo.failureReason(),
                        now));
    }

    /**
     * La cátedra confirmó el retén: la reserva existe y espera el teléfono.
     *
     * @param now instante del cambio
     * @return el proceso esperando el teléfono, o vacío si el estado actual no admitía este paso
     */
    public Optional<ReservationProcess> waitingForPhone(Instant now) {
        return apply(ReservationEvent.INITIAL_CONFIRMATION, now);
    }

    /** Copia del proceso con el estado y los datos que cambian. */
    private ReservationProcess copy(
            ReservationState state,
            String holdId,
            String catedraProcessId,
            Instant expiresAt,
            String failureReason,
            Instant updatedAt) {

        return new ReservationProcess(
                id,
                userId,
                externalPatientId,
                professionalId,
                date,
                startTime,
                state,
                holdId,
                catedraProcessId,
                expiresAt,
                requestKey,
                failureReason,
                createdAt,
                updatedAt);
    }
}
