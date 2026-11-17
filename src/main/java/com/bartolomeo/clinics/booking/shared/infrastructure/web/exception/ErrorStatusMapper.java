package com.bartolomeo.clinics.booking.shared.infrastructure.web.exception;

import java.util.Map;

import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
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
            Map.entry(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.PROFESSIONAL_DISABLED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_SLOT, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.SLOT_NOT_AVAILABLE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.HOLD_EXPIRED, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.EXTERNAL_PATIENT_MISMATCH, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.ALREADY_CONFIRMED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.NOT_CANCELLABLE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.INVALID_STATE_TRANSITION, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.CATEDRA_REJECTED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ErrorCode.CATEDRA_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ErrorCode.CATALOG_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ErrorCode.CATALOG_REJECTED, HttpStatus.SERVICE_UNAVAILABLE),
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
