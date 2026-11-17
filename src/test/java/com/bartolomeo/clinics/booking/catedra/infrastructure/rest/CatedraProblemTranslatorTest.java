package com.bartolomeo.clinics.booking.catedra.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Una fila por cada código de la matriz de la cátedra, y una por cada caso que no trae código.
 *
 * <p><strong>El test vive acá y no en el cliente HTTP a propósito.</strong> La decisión que hay
 * que probar es {@code (status, code) → qué excepción propia}, y esa decisión está entera en
 * {@link CatedraProblemTranslator}. Probarla por HTTP agregaría un servidor, un login y un
 * stub para terminar ejercitando exactamente las mismas líneas —y cubriendolas peor, porque un
 * test por fila se vuelve insoportable de leer. La prueba de que el HTTP llega hasta acá está
 * en {@code CatedraAppointmentsContractTest}, que levanta el cliente real.
 *
 * <p><strong>Qué mide.</strong> Que ningún código quede sin traducir, que cada uno produzca la
 * excepción y el {@code code} que el contrato de este servicio promete, y que los códigos que
 * este servicio decide <em>no</em> interpretar terminen en el cajón de salida en vez de
 * inventarse un significado.
 */
@DisplayName("la matriz de errores de la catedra, fila por fila")
class CatedraProblemTranslatorTest {

    private static final CatedraCall CREAR =
            CatedraCall.crearReten(15, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30));
    private static final CatedraCall CONFIRMAR = CatedraCall.confirmarReten("hold-6c0c2f41");
    private static final CatedraCall CANCELAR = CatedraCall.cancelar("process-a92b47d0");

    private final CatedraProblemTranslator traductor =
            new CatedraProblemTranslator(new ObjectMapper());

    // ------------------------------------------------------------------ 400

    @Test
    @DisplayName("VALIDATION_ERROR se convierte en validacion propia con sus campos")
    void validacion() {
        String cuerpo = """
                {"type":"https://www.jhipster.tech/problem/constraint-violation",\
                "title":"Method argument not valid","status":400,"message":"error.validation",\
                "path":"/api/appointment-holds","code":"VALIDATION_ERROR",\
                "fieldErrors":[{"objectName":"appointmentHoldRequestDTO",\
                "field":"professionalId","message":"must not be null"}]}\
                """;

        ApplicationException excepcion = traductor.translate(400, cuerpo, CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraValidationException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        CatedraValidationException validacion = (CatedraValidationException) excepcion;
        assertThat(validacion.getFieldErrors()).containsExactly("professionalId: must not be null");
        // El objectName es el nombre de una clase del otro lado: no viaja.
        assertThat(validacion.getFieldErrors().get(0)).doesNotContain("appointmentHoldRequestDTO");
    }

    @Test
    @DisplayName("VALIDATION_ERROR sin campos sigue siendo una validacion")
    void validacionSinCampos() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "VALIDATION_ERROR"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraValidationException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("INVALID_DATE_RANGE conserva el codigo y nombra la operacion")
    void rangoDeFechasInvalido() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "INVALID_DATE_RANGE"),
                CatedraCall.ocupaciones(15), causa());

        assertThat(excepcion).isInstanceOf(InvalidDateRangeException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(excepcion.getMessage()).contains("ocupaciones");
    }

    @Test
    @DisplayName("PROFESSIONAL_DISABLED no se disfraza de no encontrado")
    void profesionalDeshabilitado() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "PROFESSIONAL_DISABLED"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(ProfessionalDisabledException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.PROFESSIONAL_DISABLED);
        assertThat(excepcion.getMessage()).contains("15");
    }

    @Test
    @DisplayName("PROFESSIONAL_DISABLED desde confirmar, que no conoce el id, no se rompe")
    void profesionalDeshabilitadoSinId() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "PROFESSIONAL_DISABLED"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(ProfessionalDisabledException.class);
        assertThat(excepcion.getMessage()).doesNotContain("null");
    }

    @Test
    @DisplayName("INVALID_SLOT es distinto de \"ya ocupado\": el usuario elige otro momento")
    void slotInvalido() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "INVALID_SLOT"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(InvalidSlotException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.INVALID_SLOT);
        assertThat(excepcion.getMessage()).contains("2026-09-14").contains("09:30");
    }

    @Test
    @DisplayName("EXTERNAL_PATIENT_MISMATCH es error nuestro de coincidencia, no de formato")
    void pacienteDistinto() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "EXTERNAL_PATIENT_MISMATCH"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(ExternalPatientMismatchException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_PATIENT_MISMATCH);
        assertThat(excepcion.getMessage()).contains("hold-6c0c2f41");
    }

    // ------------------------------------------------------------------ 401 y 403

    @Test
    @DisplayName("401 sin code es rechazo nuestro de credencial y responde 503, no 401")
    void unauthorizedSinCode() {
        ApplicationException excepcion = traductor.translate(
                401, conTitulo(401, "Unauthorized"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraRejectedException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_REJECTED);
    }

    @Test
    @DisplayName("403 sin code tambien es rechazo: el contrato avisa que puede no traer code")
    void forbiddenSinCode() {
        ApplicationException excepcion = traductor.translate(
                403, conTitulo(403, "Forbidden"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraRejectedException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_REJECTED);
    }

    @Test
    @DisplayName("STUDENT_INTEGRATION_NOT_FOUND es cuenta sin integracion, no falta de permisos")
    void integracionTecnicaAusente() {
        ApplicationException excepcion = traductor.translate(
                403, problema(403, "STUDENT_INTEGRATION_NOT_FOUND"),
                CatedraCall.listarReservas(), causa());

        assertThat(excepcion).isInstanceOf(CatedraIntegrationMissingException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_REJECTED);
    }

    @Test
    @DisplayName("HOLD_OWNER_MISMATCH es 403, no 409: no es que no exista, es que no es nuestro")
    void retenAjeno() {
        ApplicationException excepcion = traductor.translate(
                403, problema(403, "HOLD_OWNER_MISMATCH"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(HoldOwnerMismatchException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("RESERVATION_OWNER_MISMATCH es 403 y nombra al proceso")
    void reservaAjena() {
        ApplicationException excepcion = traductor.translate(
                403, problema(403, "RESERVATION_OWNER_MISMATCH"), CANCELAR, causa());

        assertThat(excepcion).isInstanceOf(ReservationOwnerMismatchException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(excepcion.getMessage()).contains("process-a92b47d0");
    }

    // ------------------------------------------------------------------ 404

    @Test
    @DisplayName("PROFESSIONAL_NOT_FOUND comparte code con el catalogo, clase aparte")
    void profesionalInexistente() {
        ApplicationException excepcion = traductor.translate(
                404, problema(404, "PROFESSIONAL_NOT_FOUND"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(ProfessionalNotFoundException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("HOLD_NOT_FOUND es 404 con code de no encontrado")
    void retenInexistente() {
        ApplicationException excepcion = traductor.translate(
                404, problema(404, "HOLD_NOT_FOUND"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(HoldNotFoundException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("HOLD_EXPIRED tiene code propio porque el turno volvio a estar libre")
    void retenVencido() {
        ApplicationException excepcion = traductor.translate(
                404, problema(404, "HOLD_EXPIRED"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(HoldExpiredException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.HOLD_EXPIRED);
        assertThat(excepcion.getErrorCode()).isNotEqualTo(ErrorCode.NOT_FOUND);
        assertThat(excepcion.getMessage()).contains("hold-6c0c2f41");
    }

    @Test
    @DisplayName("RESERVATION_NOT_FOUND nombra al proceso que se quiso cancelar")
    void reservaInexistente() {
        ApplicationException excepcion = traductor.translate(
                404, problema(404, "RESERVATION_NOT_FOUND"), CANCELAR, causa());

        assertThat(excepcion).isInstanceOf(ReservationNotFoundException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(excepcion.getMessage()).contains("process-a92b47d0");
    }

    // ------------------------------------------------------------------ 409

    @Test
    @DisplayName("SLOT_ALREADY_RESERVED es 409 y code de turno no disponible")
    void turnoConfirmadoPorOtro() {
        ApplicationException excepcion = traductor.translate(
                409, problema(409, "SLOT_ALREADY_RESERVED"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(SlotAlreadyReservedException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.SLOT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("SLOT_ALREADY_HELD comparte code con el anterior, porque el usuario hace lo mismo")
    void turnoRetenidoPorOtro() {
        ApplicationException excepcion = traductor.translate(
                409, problema(409, "SLOT_ALREADY_HELD"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(SlotAlreadyHeldException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.SLOT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("RESERVATION_PROCESS_ALREADY_CONFIRMED desde confirmar usa el reten conocido")
    void procesoYaConfirmado() {
        ApplicationException excepcion = traductor.translate(
                409, problema(409, "RESERVATION_PROCESS_ALREADY_CONFIRMED"), CONFIRMAR, causa());

        assertThat(excepcion).isInstanceOf(ReservationAlreadyConfirmedException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CONFIRMED);
        assertThat(excepcion.getMessage()).contains("hold-6c0c2f41");
    }

    @Test
    @DisplayName("RESERVATION_NOT_CANCELLABLE es 409 y nombra al proceso")
    void reservaNoCancelable() {
        ApplicationException excepcion = traductor.translate(
                409, problema(409, "RESERVATION_NOT_CANCELLABLE"), CANCELAR, causa());

        assertThat(excepcion).isInstanceOf(ReservationNotCancellableException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.NOT_CANCELLABLE);
    }

    // ------------------------------------------------------------------ sin code

    @Test
    @DisplayName("503 de infraestructura sin code es no disponible")
    void infraestructuraCaidida() {
        ApplicationException excepcion = traductor.translate(503, "", CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraUnavailableException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_UNAVAILABLE);
    }

    @Test
    @DisplayName("500 de servidor sin code es no disponible, y conserva la causa")
    void servidorRoto() {
        RuntimeException original = causa();
        ApplicationException excepcion = traductor.translate(
                500, conTitulo(500, "Internal Server Error"), CREAR, original);

        assertThat(excepcion).isInstanceOf(CatedraUnavailableException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_UNAVAILABLE);
        assertThat(excepcion.getCause()).isSameAs(original);
    }

    @Test
    @DisplayName("un body que no es problem+json no adivina: es no disponible")
    void bodyIlegible() {
        ApplicationException excepcion = traductor.translate(
                502, "<html><body>Bad Gateway</body></html>", CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraUnavailableException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_UNAVAILABLE);
    }

    // ------------------------------------------------------------------ lo que no se traduce

    @Test
    @DisplayName("un codigo que no esta en la matriz no se inventa significado")
    void codigoDesconocido() {
        ApplicationException excepcion = traductor.translate(
                400, problema(400, "ALGO_QUE_NO_CONOCEMOS"), CREAR, causa());

        assertThat(excepcion).isInstanceOf(CatedraUnavailableException.class);
        assertThat(excepcion.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_UNAVAILABLE);
        assertThat(excepcion.getMessage()).contains("ALGO_QUE_NO_CONOCEMOS");
    }

    @Test
    @DisplayName("los codigos de registro no se traducen: booking no registra cuentas tecnicas")
    void codigosDeRegistro() {
        // USERNAME_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, STUDENT_GROUP_ALREADY_EXISTS,
        // INVALID_GROUP_ID e INVALID_PASSWORD pertenecen a POST /api/student/register, que este
        // servicio no llama. Se comprueba que no les inventamos una fila: caen en el cajon de
        // salida igual que cualquier codigo desconocido.
        String[] sinTraduccion = {
            "USERNAME_ALREADY_EXISTS", "EMAIL_ALREADY_EXISTS", "STUDENT_GROUP_ALREADY_EXISTS",
            "INVALID_GROUP_ID", "INVALID_PASSWORD"
        };

        for (String code : sinTraduccion) {
            ApplicationException excepcion = traductor.translate(
                    400, problema(400, code), CREAR, causa());

            assertThat(excepcion)
                    .as("code %s", code)
                    .isInstanceOf(CatedraUnavailableException.class);
        }
    }

    @Test
    @DisplayName("el 404 de STUDENT_INTEGRATION_NOT_FOUND no se traduce: es GET /student/integration")
    void integracionActualNoLlamado() {
        // Ese 404 es la fila de GET /api/student/integration, que booking no llama. El mismo
        // codigo con 403 —que si le alcanza— esta traducido, arriba.
        ApplicationException excepcion = traductor.translate(
                404, problema(404, "STUDENT_INTEGRATION_NOT_FOUND"),
                CatedraCall.listarReservas(), causa());

        assertThat(excepcion).isInstanceOf(CatedraUnavailableException.class);
    }

    // ------------------------------------------------------------------ la regla

    @Test
    @DisplayName("decide por el codigo y no por el texto del detalle, aunque contradiga")
    void ignoraElDetalle() {
        String cuerpo = """
                {"status":409,"detail":"El hold vencio hace tres dias",\
                "code":"SLOT_ALREADY_HELD"}\
                """;

        ApplicationException excepcion = traductor.translate(409, cuerpo, CREAR, causa());

        // El detalle dice una cosa y el code otra: manda el code.
        assertThat(excepcion).isInstanceOf(SlotAlreadyHeldException.class);
        assertThat(excepcion.getMessage()).doesNotContain("tres dias");
    }

    @Test
    @DisplayName("sin code, decide por el status: 403 no se convierte en 401 ni en 500")
    void sinCodeDecidePorStatus() {
        ApplicationException rechazo = traductor.translate(403, problema(403, null), CREAR, causa());
        ApplicationException indisponible = traductor.translate(504, "", CREAR, causa());

        assertThat(rechazo.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_REJECTED);
        assertThat(indisponible.getErrorCode()).isEqualTo(ErrorCode.CATEDRA_UNAVAILABLE);
    }

    // ------------------------------------------------------------------ helpers

    /** El problema mínimo que manda la matriz: status y, si lo hay, code. */
    private static String problema(int status, String code) {
        return code == null
                ? "{\"status\":" + status + "}"
                : "{\"status\":" + status + ",\"code\":\"" + code + "\"}";
    }

    /** El mismo, con el título que Spring Security pone cuando no manda code. */
    private static String conTitulo(int status, String titulo) {
        return "{\"status\":" + status + ",\"title\":\"" + titulo + "\"}";
    }

    private static RuntimeException causa() {
        return new RuntimeException("fallo original");
    }
}
