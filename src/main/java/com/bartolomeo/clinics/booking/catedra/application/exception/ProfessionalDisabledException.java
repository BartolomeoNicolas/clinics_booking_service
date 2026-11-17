package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El profesional existe pero no está habilitado para recibir reservas.
 *
 * <p><strong>No es un "no encontrado"</strong>: la diferencia es accionable. Un profesional que
 * no existe no va a volver mañana y no hay nada que ofrecerle al usuario; uno deshabilitado
 * sí puede estarlo por un motivo transitorio, y decirle al usuario "por el momento no está
 * tomando reservas" es cierto y distinto de "no existe". Mezclar los dos en {@code not-found}
 * le sacaría a la app la posibilidad de elegir qué mostrar.
 *
 * <p>Viene con status 400 de la cátedra y lo conserva: es el pedido lo que no puede cumplirse
 * en el estado actual, no el recurso.
 */
public class ProfessionalDisabledException extends ApplicationException {

    /**
     * @param referencia el id del profesional cuando la operación lo conoce, o una frase que
     *                   lo señale cuando vino de confirmar
     */
    public ProfessionalDisabledException(String referencia) {
        super(ErrorCode.PROFESSIONAL_DISABLED,
                "El profesional " + referencia + " no está habilitado para reservar.");
    }
}
