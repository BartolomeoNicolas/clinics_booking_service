package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * No se pudo obtener una respuesta utilizable de la cátedra.
 *
 * <p><strong>Es el cajón de salida de todo lo que no tiene código propio</strong>, y eso está
 * decidido, no es un descarto: los 503 de infraestructura y los 500 de servidor del anexo 13
 * vienen <em>sin garantía de {@code code}</em>, un código que este servicio nunca vio cae en la
 * misma bolsa porque reintentar con algo desconocido tampoco es una opción, y una conexión
 * rechazada o un cuerpo ilegible no tienen status que traducir.
 *
 * <p>Lo que no se hace es inventar la causa: la excepción conserva el status original, el cuerpo
 * cuando lo había y la excepción de red, así que en el log se distingue si fue un 500, un 503,
 * un timeout o un JSON que no era el que esperábamos.
 *
 * <p>503 de booking: "ahora mismo no se puede", que es lo que la app necesita saber para
 * ofrecer un reintento en vez de mostrar un error definitivo.
 */
public class CatedraUnavailableException extends ApplicationException {

    public CatedraUnavailableException(String message) {
        super(ErrorCode.CATEDRA_UNAVAILABLE, message);
    }

    public CatedraUnavailableException(String message, Throwable cause) {
        super(ErrorCode.CATEDRA_UNAVAILABLE, message, cause);
    }
}
