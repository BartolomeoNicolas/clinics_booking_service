package com.bartolomeo.clinics.booking.catalog.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El servicio de catálogo no está disponible.
 *
 * <p><strong>Es un error de este servicio hacia el cliente, no del catálogo hacia nosotros.</strong>
 * El catálogo cayó, o tardó más de lo que se le tiene paciencia, o contestó 5xx: en los tres
 * casos esta llamada no puede responder lo que se le preguntó. Se traduce a 503 con el código
 * estable {@code catalog-unavailable}, que es lo que un cliente necesita para distinguir
 * "esto no está disponible ahora" de "esto está mal hecho".
 *
 * <p><strong>No es un error de negocio y no debería producirse en el camino feliz.</strong> Si
 * aparece en una prueba, el problema es de infraestructura o de configuración, no de la regla
 * que se estaba aplicando.
 */
public class CatalogUnavailableException extends ApplicationException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(ErrorCode.CATALOG_UNAVAILABLE, message, cause);
    }

    public CatalogUnavailableException(String message) {
        super(ErrorCode.CATALOG_UNAVAILABLE, message);
    }
}
