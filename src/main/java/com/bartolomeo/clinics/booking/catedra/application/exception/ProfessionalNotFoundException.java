package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La cátedra no conoce al profesional sobre el que se quiso crear o confirmar un retén.
 *
 * <p>Da lo mismo que el catálogo no lo haya devuelto o que haya dejado de existir entre una
 * consulta y otra: para quien reserva, es "ese profesional no está". Por eso lleva el mismo
 * código {@code not-found} que trae la excepción equivalente del contexto {@code catalog}, y
 * el cliente no tiene que distinguir de qué servicio vino la respuesta.
 *
 * <p><strong>No es la misma clase que la del contexto {@code catalog}</strong>, aunque el
 * nombre coincida: cada una habla de un contrato distinto, con su mensaje y su servicio, y que
 * estén en paquetes distintos es lo que evita que un cambio en un contrato se cuele en el
 * otro. Ese cruce —{@code catedra} dependiendo de {@code catalog}— sería exactamente lo que la
 * separación de contexts existe para impedir.
 */
public class ProfessionalNotFoundException extends ApplicationException {

    /**
     * @param referencia el id del profesional cuando la operación lo conoce, o una frase que
     *                   lo señale —"de este retén"— cuando el error vino de confirmar, donde
     *                   solo se manda el identificador del retén
     */
    public ProfessionalNotFoundException(String referencia) {
        super(ErrorCode.NOT_FOUND,
                "La cátedra no conoce el profesional " + referencia + ".");
    }
}
