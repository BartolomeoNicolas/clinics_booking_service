package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * La cátedra no nos dejó entrar: rechazó la credencial con la que la llamamos.
 *
 * <p>Abarca el 401 —token vencido o firma que no cierra— y los 403 de Spring Security que
 * llegan <strong>sin {@code code}</strong>, que es lo que advierte el anexo 13. Es un caso
 * típico de error que no se puede decidir por código: no lo trae, así que la decisión es por el
 * status, que es la otra mitad de la regla.
 *
 * <p>Como en el catálogo, se responde 503 y no 401: el 401 le diría al usuario final que <em>él</em>
 * no está autenticado, que es falso —él lo está, somos nosotros los que no pudimos presentar
 * una credencial válida—, y la app cerraría su sesión por un problema que no es suya.
 *
 * <p>La causa original queda adjunta porque sin ella no se distingue un token vencido de una
 * clave pública desalineada, que son dos diagnósticos distintos.
 */
public class CatedraRejectedException extends ApplicationException {

    public CatedraRejectedException(String message, Throwable cause) {
        super(ErrorCode.CATEDRA_REJECTED, message, cause);
    }
}
