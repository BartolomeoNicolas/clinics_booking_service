package com.bartolomeo.clinics.booking.infrastructure.web.exception;

import java.util.Map;

import com.bartolomeo.clinics.booking.application.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Decide con que status HTTP responde cada {@link ErrorCode}.
 *
 * <p>El codigo del error lo define el dominio ({@code user.application.exception.ErrorCode}) y
 * el status con el que se responde lo decidimos aca. Mantener las dos cosas juntas obligaria
 * a que {@code application} dependiera de {@code infrastructure}, que es justo la dependencia
 * que la arquitectura hexagonal prohibe.
 *
 * <p>Es un mapa cerrado a proposito: si un error nuevo no esta aca, la aplicacion falla al
 * arrancar en vez de inventarse un status.
 */
public final class ErrorStatusMapper {

    private static final Map<ErrorCode, HttpStatus> STATUS = Map.ofEntries(
            Map.entry(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.LOGIN_ALREADY_TAKEN, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.EMAIL_ALREADY_TAKEN, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
            Map.entry(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

    private ErrorStatusMapper() {
    }

    public static HttpStatus toHttpStatus(ErrorCode code) {
        HttpStatus status = STATUS.get(code);
        if (status == null) {
            throw new IllegalStateException(
                    "ErrorCode." + code + " no tiene status HTTP mapeado. "
                    + "Agregalo en ErrorStatusMapper: el mapeo es cerrado a proposito.");
        }
        return status;
    }
}
