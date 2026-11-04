package com.bartolomeo.clinics.booking.shared.application.exception;

/**
 * Excepcion base de la capa de aplicacion.
 *
 * <p>Nace en {@code application/exception}, sin dependencias de Spring ni de web: es una regla
 * del negocio, no un detalle del transporte. Quien la traduce a una respuesta HTTP es
 * {@code infrastructure.web.exception.GlobalExceptionHandler}.
 *
 * <p>Cada subclase lleva un {@link ErrorCode} fijo, y ese codigo es parte del contrato.
 */
public abstract class ApplicationException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Con la causa original adjunta.
     *
     * <p>Hace falta cuando el fallo viene de afuera —una llamada HTTP, por ejemplo—: sin la
     * causa, el 503 dice que el catálogo no está disponible pero no si fue una conexión
     * rechazada, un timeout o un 500, que son tres cosas distintas para diagnosticar.
     */
    protected ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
