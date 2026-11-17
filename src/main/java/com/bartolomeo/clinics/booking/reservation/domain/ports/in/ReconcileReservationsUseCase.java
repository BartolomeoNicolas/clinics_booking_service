package com.bartolomeo.clinics.booking.reservation.domain.ports.in;

/**
 * Converge los procesos pendientes con el estado real de la cátedra.
 */
public interface ReconcileReservationsUseCase {

    /**
     * Revisa los procesos que todavía no llegaron a un estado final y los deja donde tienen que
     * estar.
     *
     * <p><strong>Un proceso vencido se cierra.</strong> Su vigencia la determina la cátedra y
     * ya pasó: el proceso queda vencido y deja de admitir intentos.
     *
     * <p><strong>Un proceso que la cátedra no conoce se cierra solo si esperaba una
     * reserva.</strong> Si el proceso está esperando el teléfono o esperando el resultado,
     * la reserva tenía que existir; si no existe, el retén venció y el proceso se cae. Un
     * proceso que todavía tiene el retén vivo —solicitado, reten creado, teléfono rechazado—
     * no se toca: puede seguir su curso.
     *
     * <p><strong>Un proceso que la cátedra conoce se mueve a donde ella dice.</strong> Si para
     * la cátedra la reserva está confirmada, cancelada o fallida, el proceso local queda en
     * ese estado, aunque el evento que lo dice se haya perdido.
     *
     * @return cuántos procesos cambiaron de estado
     */
    int reconcilePending();
}
