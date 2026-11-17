package com.bartolomeo.clinics.booking.catedra.infrastructure.rest;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraIntegrationMissingException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraRejectedException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraValidationException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ExternalPatientMismatchException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldExpiredException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldNotFoundException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldOwnerMismatchException;
import com.bartolomeo.clinics.booking.catedra.application.exception.InvalidDateRangeException;
import com.bartolomeo.clinics.booking.catedra.application.exception.InvalidSlotException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ProfessionalDisabledException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ProfessionalNotFoundException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationAlreadyConfirmedException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationNotCancellableException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationNotFoundException;
import com.bartolomeo.clinics.booking.catedra.application.exception.ReservationOwnerMismatchException;
import com.bartolomeo.clinics.booking.catedra.application.exception.SlotAlreadyHeldException;
import com.bartolomeo.clinics.booking.catedra.application.exception.SlotAlreadyReservedException;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.CatedraProblemResponse;
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Convierte un error de la cátedra en una excepción propia, mirando {@code status} y
 * {@code code} y nunca el texto de {@code detail}.
 *
 * <p><strong>Esta es la regla que hace que los dos servicios hablen el mismo idioma.</strong> La
 * cátedra advierte que sus mensajes son texto libre, que cambian y que están en su vocabulario
 * —{@code objectName: appointmentHoldRequestDTO}—; quien decide qué pasó es la pareja
 * {@code status} + {@code code}, que es estable. Por eso en este archivo no aparece
 * {@code detail} en ninguna condición: solo se adjunta a la causa para el log.
 *
 * <p><strong>Una fila por código de la matriz, y una fila para todo lo que no tiene
 * código.</strong> Los 503, los 500, los códigos que este servicio nunca vio y los cuerpos que
 * no son {@code problem+json} caen todos en {@link CatedraUnavailableException}, que dice la
 * verdad —no supimos qué pasó— en vez de adivinar un significado. Adivinar es peor: decirle a
 * la app "ese turno no existe" cuando en realidad no entendimos la respuesta la haría mostrar
 * algo falso.
 *
 * <p>La tabla completa —código de la cátedra → excepción → código de booking → status— está en
 * el {@code README}, y hay un test por cada fila que este servicio sí puede recibir.
 *
 * <p>Sin estado y sin acceso al puerto: es una función pura sobre la respuesta. Eso es lo que
 * permite cubrir las veinte filas con tests de unidad, sin levantar la app ni un contenedor.
 */
@Component
@RequiredArgsConstructor
public class CatedraProblemTranslator {

    private static final Logger log = LoggerFactory.getLogger(CatedraProblemTranslator.class);

    /** Códigos de la matriz que este servicio traduce. Los demás caen en el cajón de salida. */
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    private static final String STUDENT_INTEGRATION_NOT_FOUND = "STUDENT_INTEGRATION_NOT_FOUND";
    private static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    private static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    private static final String INVALID_SLOT = "INVALID_SLOT";
    private static final String SLOT_ALREADY_RESERVED = "SLOT_ALREADY_RESERVED";
    private static final String SLOT_ALREADY_HELD = "SLOT_ALREADY_HELD";
    private static final String HOLD_NOT_FOUND = "HOLD_NOT_FOUND";
    private static final String HOLD_EXPIRED = "HOLD_EXPIRED";
    private static final String HOLD_OWNER_MISMATCH = "HOLD_OWNER_MISMATCH";
    private static final String EXTERNAL_PATIENT_MISMATCH = "EXTERNAL_PATIENT_MISMATCH";
    private static final String RESERVATION_PROCESS_ALREADY_CONFIRMED =
            "RESERVATION_PROCESS_ALREADY_CONFIRMED";
    private static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    private static final String RESERVATION_OWNER_MISMATCH = "RESERVATION_OWNER_MISMATCH";
    private static final String RESERVATION_NOT_CANCELLABLE = "RESERVATION_NOT_CANCELLABLE";

    private final ObjectMapper objectMapper;

    /**
     * Del error que contestó la cátedra a la excepción propia que se le devuelve a afuera.
     *
     * <p><strong>La causa se adjunta solo donde cambia el diagnóstico</strong>: en las de
     * no disponible y de rechazo, porque ahí sin ella no se distingue un timeout de un 500 o un
     * token vencido de una clave desalineada. En la matriz de errores de negocio no, porque
     * {@code status} + {@code code} ya dicen qué pasó y apilar una excepción de HTTP debajo de
     * cada 404 solo ensucia el log.
     *
     * @param status status HTTP de la respuesta
     * @param body   cuerpo {@code problem+json}, o lo que haya; puede venir vacío
     * @param call   qué se le estaba pidiendo, para nombrar cosas reales en el mensaje
     * @param cause  la excepción original
     * @return la excepción propia, siempre
     */
    public ApplicationException translate(int status, String body, CatedraCall call,
            Throwable cause) {
        CatedraProblemResponse problema = parse(body);
        String code = problema == null ? null : problema.code();

        if (status == 401) {
            // Sin code: el contrato advierte que los rechazos de Spring Security pueden no
            // traerlo. Se decide por el status, que es la otra mitad de la regla.
            return new CatedraRejectedException(
                    "La cátedra no nos dejó pasar: rechazó la credencial de este servicio"
                            + " (HTTP 401).",
                    cause);
        }
        if (status == 403) {
            if (STUDENT_INTEGRATION_NOT_FOUND.equals(code)) {
                return new CatedraIntegrationMissingException(cause);
            }
            if (HOLD_OWNER_MISMATCH.equals(code)) {
                return new HoldOwnerMismatchException(
                        call.holdId() != null ? call.holdId() : "pedido");
            }
            if (RESERVATION_OWNER_MISMATCH.equals(code)) {
                return new ReservationOwnerMismatchException(
                        call.reservationProcessId() != null
                                ? call.reservationProcessId() : "pedido");
            }
            return new CatedraRejectedException(
                    "La cátedra no nos dejó pasar: rechazó la credencial de este servicio"
                            + " (HTTP 403).",
                    cause);
        }
        if (status == 400) {
            if (VALIDATION_ERROR.equals(code)) {
                return validacion(problema);
            }
            if (INVALID_DATE_RANGE.equals(code)) {
                return new InvalidDateRangeException(
                        "La cátedra rechazó el rango de fechas de " + call.operation()
                                + ": el inicio es posterior al fin.");
            }
            if (PROFESSIONAL_DISABLED.equals(code)) {
                return new ProfessionalDisabledException(call.professionalRef());
            }
            if (INVALID_SLOT.equals(code)) {
                return new InvalidSlotException(
                        "El turno " + call.turnoRef() + " del profesional "
                                + call.professionalRef()
                                + " no cae en la agenda habilitada.");
            }
            if (EXTERNAL_PATIENT_MISMATCH.equals(code)) {
                return new ExternalPatientMismatchException(
                        call.holdId() != null ? call.holdId() : "pedido");
            }
            return desconocido(status, code, call, cause);
        }
        if (status == 404) {
            if (PROFESSIONAL_NOT_FOUND.equals(code)) {
                return new ProfessionalNotFoundException(call.professionalRef());
            }
            if (HOLD_NOT_FOUND.equals(code)) {
                return new HoldNotFoundException(
                        call.holdId() != null ? call.holdId() : "pedido");
            }
            if (HOLD_EXPIRED.equals(code)) {
                return new HoldExpiredException(
                        call.holdId() != null ? call.holdId() : "pedido");
            }
            if (RESERVATION_NOT_FOUND.equals(code)) {
                return new ReservationNotFoundException(
                        call.reservationProcessId() != null
                                ? call.reservationProcessId() : "pedido");
            }
            return desconocido(status, code, call, cause);
        }
        if (status == 409) {
            if (SLOT_ALREADY_RESERVED.equals(code)) {
                return new SlotAlreadyReservedException(
                        "El turno " + call.turnoRef() + " del profesional "
                                + call.professionalRef()
                                + " ya está confirmado por otra reserva.");
            }
            if (SLOT_ALREADY_HELD.equals(code)) {
                return new SlotAlreadyHeldException(
                        "El turno " + call.turnoRef() + " del profesional "
                                + call.professionalRef()
                                + " está retenido por otro proceso en curso.");
            }
            if (RESERVATION_PROCESS_ALREADY_CONFIRMED.equals(code)) {
                return new ReservationAlreadyConfirmedException(
                        call.reservationProcessId() != null
                                ? "El proceso " + call.reservationProcessId()
                                        + " ya estaba confirmado."
                                : "El retén " + call.holdId() + " ya estaba confirmado.");
            }
            if (RESERVATION_NOT_CANCELLABLE.equals(code)) {
                return new ReservationNotCancellableException(
                        call.reservationProcessId() != null
                                ? call.reservationProcessId() : "pedido");
            }
            return desconocido(status, code, call, cause);
        }

        // 503 de infraestructura, 500 de servidor y cualquier otro status que la cátedra
        // mande sin que este servicio tenga una fila para él: los tres dicen lo mismo
        // desde afuera —no se pudo hablar con la cátedra—, y por eso comparten excepción.
        return new CatedraUnavailableException(
                "La cátedra no devolvió una respuesta utilizable: HTTP " + status
                        + (code == null ? "" : ", code " + code) + " en " + call.operation() + ".",
                cause);
    }

    /**
     * Un código que este servicio nunca vio.
     *
     * <p>Se queda con el status y el código en el mensaje, y devuelve
     * {@link CatedraUnavailableException} en lugar de adivinar. Es la decisión consciente de
     * este archivo: no saber qué significa una respuesta es, para el cliente, exactamente lo
     * mismo que no haberla recibido.
     */
    private CatedraUnavailableException desconocido(int status, String code, CatedraCall call,
            Throwable cause) {
        log.warn("La cátedra devolvió un código que no está en la matriz: HTTP {}, code {}, en {}",
                status, code, call.operation());
        return new CatedraUnavailableException(
                "La cátedra devolvió HTTP " + status + " con el código " + code
                        + ", que este servicio no sabe interpretar, al pedir "
                        + call.operation() + ".",
                cause);
    }

    /**
     * La validación, con los campos que fallaron.
     *
     * <p>Los nombres de campo sí viajan: son los que la app puede relacionar con lo que mandó.
     * El {@code objectName} no, porque es el nombre de una clase del otro lado. Y el mensaje
     * top-level se escribe acá en nuestro idioma en lugar de copiar el suyo.
     */
    private CatedraValidationException validacion(CatedraProblemResponse problema) {
        List<CatedraProblemResponse.CatedraFieldError> errores = problema.fieldErrors() == null
                ? List.of()
                : problema.fieldErrors();
        List<String> campos = errores.stream()
                .filter(error -> error.field() != null && !error.field().isBlank())
                .map(error -> error.field() + ": "
                        + (error.message() == null || error.message().isBlank()
                                ? "valor inválido"
                                : error.message()))
                .toList();

        String mensaje = campos.isEmpty()
                ? "La cátedra rechazó el cuerpo del pedido: falta un campo, un formato o una"
                        + " longitud inválida."
                : "La cátedra rechazó el cuerpo del pedido: " + String.join("; ", campos);

        return new CatedraValidationException(mensaje, campos);
    }

    /**
     * Lee el cuerpo del error si se puede.
     *
     * <p>Si no se puede —vacío, no es JSON, no es un problema— devuelve nulo y la decisión
     * sigue siendo por el status, que es lo único que queda. Ese caso no es raro: un 503 de
     * infraestructura a veces viene con una página HTML y ningún código.
     */
    private CatedraProblemResponse parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, CatedraProblemResponse.class);
        } catch (RuntimeException ex) {
            log.warn("La cátedra devolvió un cuerpo de error que no es problem+json: {}",
                    truncar(body));
            return null;
        }
    }

    /** Un trozo corto para el log: el cuerpo de error nunca trae secretos, pero tampoco hace
     * falta volcarlo entero. */
    private static String truncar(String body) {
        String limpio = body.replace('\n', ' ').replace('\r', ' ');
        return limpio.length() <= 300 ? limpio : limpio.substring(0, 300) + "…";
    }
}
