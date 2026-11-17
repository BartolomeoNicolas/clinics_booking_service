package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El turno ya está confirmado por la reserva de otra persona.
 *
 * <p>Comparte código con {@link SlotAlreadyHeldException} porque, desde afuera, es la misma
 * noticia: <em>ese turno no lo vas a poder tomar</em>. La app no hace nada distinto en un caso
 * que en el otro, y el contrato advierte que decidir por el texto de {@code detail} es un error
 * —por eso los clientes miran código—, así que duplicar el código para distinguirlos sería
 * invitar a que alguien lo haga.
 *
 * <p>Se le da clase propia porque al log le sirve: "confirmado por otra" y "retenido por otra"
 * son dos carreras distintas y una dice algo sobre cuánto demora el otro lado en liberar.
 *
 * <p>409, como en la cátedra: hay conflicto con un estado existente y repetir el mismo pedido
 * ahora no va a resolverlo.
 *
 * @param message el turno en juego, nombrado por quien traduce: crear conoce el profesional y
 *                la hora, confirmar solo el retén
 */
public class SlotAlreadyReservedException extends ApplicationException {

    public SlotAlreadyReservedException(String message) {
        super(ErrorCode.SLOT_NOT_AVAILABLE, message);
    }
}
