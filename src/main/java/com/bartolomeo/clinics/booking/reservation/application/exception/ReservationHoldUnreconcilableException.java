package com.bartolomeo.clinics.booking.reservation.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El retén se creó del otro lado pero no se pudo reconciliar.
 *
 * <p><strong>Es el caso raro que hay que saber nombrar.</strong> La llamada de creación del
 * retén se perdió —timeout, respuesta que no llegó— y al reconciliar aparece que el turno
 * <em>sí</em> quedó tomado en la cátedra, pero sin que este servicio conozca su
 * {@code holdId}: no se puede confirmar y repetir crearía un retén nuevo sobre el mismo
 * turno.
 *
 * <p>Lo que se hace es no repetir y dejar que el retén venza solo. El proceso queda en
 * {@code FAILED} y el cliente recibe un 503 con el código {@code catedra-unavailable}: la
 * operación no se pudo completar, pero la situación es recuperable —el retén vence y el turno
 * vuelve a estar disponible— y no se le está mintiendo con un error de dirección.
 *
 * <p>Repetir la creación a ciegas habría sido más rápido y habría dejado dos retenes.
 */
public class ReservationHoldUnreconcilableException extends ApplicationException {

    public ReservationHoldUnreconcilableException(String detail) {
        super(ErrorCode.CATEDRA_UNAVAILABLE,
                "El retén se creó en la cátedra pero no se pudo reconciliar: " + detail
                        + ". El turno queda tomado hasta que el retén venza.");
    }
}
