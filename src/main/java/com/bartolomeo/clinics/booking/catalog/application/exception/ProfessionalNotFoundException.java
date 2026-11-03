package com.bartolomeo.clinics.booking.catalog.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El profesional no existe en el catálogo.
 *
 * <p>El catálogo contesta 404 y se traduce a este error con el código estable
 * {@code not-found} y status 404, que es la misma respuesta que daría un recurso propio que
 * no existe. Para quien llama la distinción no aporta: "no hay nada que mostrar" es lo mismo
 * venga de acá o de allá.
 *
 * <p><strong>No es lo mismo que una lista vacía.</strong> Pedir la agenda de un profesional
 * que no está es una pregunta mal hecha —el id no vino del catálogo— y responder "no tiene
 * horarios" diría lo contrario de lo que pasó.
 */
public class ProfessionalNotFoundException extends ApplicationException {

    public ProfessionalNotFoundException(long professionalId) {
        super(ErrorCode.NOT_FOUND,
                "No existe el profesional " + professionalId + " en el catálogo.");
    }
}
