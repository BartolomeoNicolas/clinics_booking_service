package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El JWT que mandamos llegó bien firmado pero sin una integración técnica detrás.
 *
 * <p>La cátedra responde 403 con {@code STUDENT_INTEGRATION_NOT_FOUND} cuando el token es
 * válido y suyo, pero la cuenta que representa no tiene integración aprovisionada. <strong>No
 * es un problema del usuario ni de sus permisos</strong>: es que este servicio se está
 * autenticando con una cuenta que todavía no está lista, y lo que hay que arreglar es la
 * configuración de este servicio, no volver a intentar.
 *
 * <p>Por eso se traduce a 503 y no a 403. Un 403 le diría a la app que el usuario no tiene
 * permiso —falso— y le diría a quien prueba la suite que algo hizo mal en la petición —también
 * falso—. 503 dice lo que es: no se pudo hablar con quien hace falta ahora mismo.
 *
 * <p>Sirve además para el caso hermano: <strong>que no tengamos las credenciales para pedir ese
 * JWT</strong>. Son la misma falla vista desde dos lugares —la cuenta no está lista, o no
 * sabemos con qué llamarla—, con el mismo arreglo: alguien tiene que configurar este servicio.
 * Por eso lleva dos constructores y no un mensaje único.
 *
 * <p>Comparte código con {@link CatedraRejectedException} porque la consecuencia es la misma:
 * la credencial no nos dejó pasar y no se arregla reintentando.
 */
public class CatedraIntegrationMissingException extends ApplicationException {

    public CatedraIntegrationMissingException(Throwable cause) {
        this("El JWT técnico de la cátedra no tiene integración técnica asociada.", cause);
    }

    public CatedraIntegrationMissingException(String message, Throwable cause) {
        super(ErrorCode.CATEDRA_REJECTED, message, cause);
    }

    public CatedraIntegrationMissingException(String message) {
        super(ErrorCode.CATEDRA_REJECTED, message);
    }
}
