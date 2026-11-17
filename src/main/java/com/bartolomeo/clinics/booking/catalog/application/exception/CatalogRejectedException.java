package com.bartolomeo.clinics.booking.catalog.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El catálogo rechazó la credencial con la que se lo llamó.
 *
 * <p><strong>No significa que el recurso esté mal: significa que no nos dejaron entrar.</strong>
 * Un 401 o un 403 del catálogo es una falla de integración de este lado —la clave pública que
 * él usa para verificar no coincide con la que firmamos, la audiencia del token no es
 * la que espera, el token venció en el camino—. Nada de eso se arregla reintentando, por eso
 * se responde 503 y no un 401: el 401 le diría al usuario final que él no está autenticado,
 * que es falso.
 *
 * <p>Se lleva la causa adjunta para poder decir en el log si fue un 401, un 403 u otra cosa.
 */
public class CatalogRejectedException extends ApplicationException {

    public CatalogRejectedException(String message, Throwable cause) {
        super(ErrorCode.CATALOG_REJECTED, message, cause);
    }

    public CatalogRejectedException(String message) {
        super(ErrorCode.CATALOG_REJECTED, message);
    }
}
