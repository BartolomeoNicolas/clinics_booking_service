package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * El cuerpo {@code application/problem+json} que manda la cátedra cuando algo sale mal.
 *
 * <p><strong>Este registro es la mitad de la regla "decidir por {@code status} y {@code code},
 * nunca por {@code detail}".</strong> De todo lo que trae, para elegir qué excepción propia
 * lanzar solo se mira {@code status} y {@code code}; {@code detail} queda adjunto en la causa
 * para poder diagnosticar desde el log, y jamás se copia al mensaje que ve el usuario, porque
 * es un texto del otro lado en el idioma y con los nombres de sus DTO.
 *
 * <p>{@code code} puede venir ausente: el contrato advierte que los rechazos de Spring
 * Security —401 y 403— a veces no lo traen. Ese caso no se resuelve mirando otro campo, se
 * resuelve con el status, que es la otra mitad de la regla.
 *
 * <p>Tolerante a campos nuevos, como todo lo que viene de afuera: si la cátedra agrega un
 * campo al problema, la traducción sigue funcionando.
 *
 * @param type        URI de tipo del problema, no se usa
 * @param title       título, no se usa
 * @param status      status HTTP repetido en el cuerpo
 * @param detail      texto libre del otro lado, solo para diagnóstico
 * @param message     mensaje corto, presente en algunas variantes
 * @param path        recurso al que se llamó
 * @param code        código funcional estable, la mitad que decide
 * @param fieldErrors errores por campo, solo en problemas de validación
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraProblemResponse(
        String type,
        String title,
        Integer status,
        String detail,
        String message,
        String path,
        String code,
        List<CatedraFieldError> fieldErrors) {

    /**
     * Un error de campo del cuerpo que mandamos.
     *
     * <p>{@code objectName} es el nombre de la clase <em>del otro lado</em> —
     * {@code appointmentHoldRequestDTO}— y por eso no se reenvía hacia la app: no le dice nada.
     * Lo que sí viaja es el campo y qué esperaba la validación, que es lo único accionable.
     *
     * @param objectName clase validada en la cátedra, se descarta al traducir
     * @param field      campo que falló
     * @param message    qué esperaba la validación
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatedraFieldError(String objectName, String field, String message) {
    }
}
