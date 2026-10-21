package com.bartolomeo.clinics.booking.infrastructure.web.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cuerpo de error en {@code application/problem+json}.
 *
 * <p>Responde al anexo §13 del contrato: {@code type}, {@code title}, {@code status},
 * {@code detail}, {@code path}, {@code message} y un {@code code} estable, mas
 * {@code fieldErrors} para validaciones.
 *
 * <p><strong>{@code detail} no es para el usuario final.</strong> Puede contener nombres
 * internos. Por eso el cliente decide por {@code status} + {@code code}: son los dos campos
 * estables, y {@code message} es la version en claro del mismo {@code code}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String path,
        String message,
        String code,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    /**
     * Error de un campo concreto de una entrada invalida.
     */
    public record FieldError(
            String field,
            String message,
            String rejectedValue) {
    }

    public ApiProblemDetail(String type, String title, int status, String detail,
                            String path, String message, String code, Instant timestamp) {
        this(type, title, status, detail, path, message, code, timestamp, List.of());
    }
}
