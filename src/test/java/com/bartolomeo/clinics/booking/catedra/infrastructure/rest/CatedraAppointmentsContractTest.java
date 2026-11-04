package com.bartolomeo.clinics.booking.catedra.infrastructure.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraRejectedException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraValidationException;
import com.bartolomeo.clinics.booking.catedra.application.exception.HoldExpiredException;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentCancellation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentConfirmation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraReservationStatus;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraProperties;
import com.bartolomeo.clinics.booking.catedra.infrastructure.security.CatedraTokenSource;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * El contrato de booking con la cátedra, pegado al cliente real.
 *
 * <p><strong>No se mockea nuestro propio código.</strong> El adaptador es el de producción, con
 * su {@code RestClient}, sus cinco timeouts, su emisor de token y su traductor de errores
 * reales; el único doble es el servidor HTTP del otro lado, que es justamente la frontera. Si
 * el test pasara con un adaptador inventado, no estaría probando el contrato sino nuestra idea
 * del contrato.
 *
 * <p><strong>Qué cubre este archivo y qué no.</strong> Acá se prueba lo que solo se puede ver
 * pasando por la red: los paths y parámetros exactos, los formatos de fecha y hora que exige el
 * contrato, el header de autorización, la renovación del token ante un 401, qué se reintenta y
 * qué no, y que un error venga de verdad del otro lado y termine en nuestra excepción. La
 * matriz entera de códigos —las veinte filas— está cubierta en
 * {@code CatedraProblemTranslatorTest}, que es donde vive la decisión.
 *
 * <p><strong>Los cuerpos salen del contrato, no de la imaginación.</strong> Cada fixture es
 * el ejemplo literal de la §8 a la §12 de {@code INTEGRATION_REFERENCE-v2.md}. Si la cátedra
 * cambia la forma de sus respuestas, hay que reemplazar estos JSON y el test se pone rojo
 * —falta un campo, cambió un tipo— antes de que llegue a producción.
 */
@DisplayName("el contrato de las reservas contra la catedra")
class CatedraAppointmentsContractTest {

    // ------------------------------------------------------------------ fixtures

    /** §8, GET /api/appointment-occupancies. */
    private static final String OCUPACIONES = """
            {"professionalId":15,"from":"2026-09-14","to":"2026-09-14","occupancies":[\
            {"date":"2026-09-14","startTime":"09:00:00","status":"CONFIRMED","expiresAt":null},\
            {"date":"2026-09-14","startTime":"09:30:00","status":"HELD",\
            "expiresAt":"2026-09-10T14:44:50Z"}]}\
            """;

    /** §9, POST /api/appointment-holds. */
    private static final String RETEN = """
            {"holdId":"hold-6c0c2f41","reservationProcessId":"process-a92b47d0",\
            "status":"HELD","expiresAt":"2026-09-10T14:44:50Z"}\
            """;

    /** §10, POST /api/appointment-holds/{holdId}/confirm. */
    private static final String CONFIRMACION = """
            {"reservationProcessId":"process-a92b47d0","status":"WAITING_FOR_PHONE",\
            "expiresAt":"2026-09-10T14:44:50Z"}\
            """;

    /** §11, GET /api/appointments. */
    private static final String LISTADO = """
            [{"reservationId":8251,"reservationProcessId":"process-a92b47d0",\
            "externalPatientId":"user-42","appointmentDate":"2026-09-14",\
            "startTime":"09:30:00","endTime":"10:00:00","professionalId":15,\
            "professionalFirstName":"Ana","professionalLastName":"Gomez",\
            "patientFirstName":"Juan","patientLastName":"Perez",\
            "patientPhone":"+5492615551234","status":"CONFIRMED",\
            "createdAt":"2026-09-10T14:35:00Z","confirmedAt":"2026-09-10T14:36:00Z",\
            "cancelledAt":null,"cancellationReason":null}]\
            """;

    /** §12, POST /api/appointments/{reservationProcessId}/cancel. */
    private static final String CANCELACION = """
            {"reservationId":8251,"reservationProcessId":"process-a92b47d0",\
            "status":"CANCELLED","cancelledAt":"2026-09-11T15:10:00Z"}\
            """;

    /** §13, problema con HOLD_EXPIRED. */
    private static final String RETEN_VENCIDO = """
            {"type":"https://www.jhipster.tech/problem/problem-with-message",\
            "title":"Not Found","status":404,"detail":"Hold expired",\
            "path":"/api/appointment-holds/hold-6c0c2f41/confirm",\
            "code":"HOLD_EXPIRED"}\
            """;

    /** §13, problema de validación con fieldErrors. */
    private static final String VALIDACION = """
            {"type":"https://www.jhipster.tech/problem/constraint-violation",\
            "title":"Method argument not valid","status":400,"message":"error.validation",\
            "path":"/api/appointment-holds","code":"VALIDATION_ERROR",\
            "fieldErrors":[{"objectName":"appointmentHoldRequestDTO",\
            "field":"externalPatientId","message":"size must be between 1 and 100"}]}\
            """;

    private static final LocalDate DIA = LocalDate.of(2026, 9, 14);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    private static WireMockServer catedra;

    private CatedraAppointmentsAdapter adapter;

    @BeforeAll
    static void levantarElServidorFalso() {
        catedra = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        catedra.start();
    }

    @AfterAll
    static void bajarElServidorFalso() {
        catedra.stop();
    }

    @BeforeEach
    void preparar() {
        catedra.resetAll();

        CatedraProperties properties = new CatedraProperties();
        properties.setBaseUrl(catedra.baseUrl());
        properties.setTechnicalUser("juan.perez");
        properties.setTechnicalPassword("una-clave-segura");

        CatedraTokenSource tokens = new CatedraTokenSource(properties);
        CatedraProblemTranslator translator = new CatedraProblemTranslator(new ObjectMapper());
        adapter = new CatedraAppointmentsAdapter(properties, tokens, translator);

        stubearLogin("jwt-1");
    }

    // ------------------------------------------------------------------ las cinco rutas

    @Test
    @DisplayName("lee ocupaciones con los tres parámetros y los dos estados del contrato")
    void leeOcupaciones() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .willReturn(ok(OCUPACIONES)));

        List<Occupancy> ocupaciones = adapter.getOccupancies(15, DIA, DIA);

        assertThat(ocupaciones).hasSize(2);
        assertThat(ocupaciones.get(0).date()).isEqualTo(DIA);
        assertThat(ocupaciones.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ocupaciones.get(0).status()).isEqualTo(OccupancyStatus.CONFIRMED);
        assertThat(ocupaciones.get(0).expiresAt()).isNull();
        assertThat(ocupaciones.get(1).status()).isEqualTo(OccupancyStatus.HELD);
        assertThat(ocupaciones.get(1).expiresAt())
                .isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    @DisplayName("crea el reten con la hora en HH:mm, como muestra el contrato")
    void creaElReten() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds")).willReturn(ok(RETEN)));

        AppointmentHold reten = adapter.createHold(15, DIA, HORA, "user-42");

        assertThat(reten.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(reten.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(reten.expiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));

        String cuerpo = cuerpoDe("POST", "/api/appointment-holds");
        // El ejemplo del contrato manda "09:30", sin segundos: un LocalTime serializado solo
        // produciría "09:30:00" y el formato del pedido tendría que adivinarse.
        assertThat(cuerpo).contains("\"startTime\":\"09:30\"");
        assertThat(cuerpo).doesNotContain("09:30:00");
        assertThat(cuerpo).contains("\"date\":\"2026-09-14\"");
        assertThat(cuerpo).contains("\"externalPatientId\":\"user-42\"");
        assertThat(cuerpo).contains("\"professionalId\":15");
    }

    @Test
    @DisplayName("confirma el reten por su id y devuelve el proceso esperando el telefono")
    void confirmaElReten() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds/hold-6c0c2f41/confirm"))
                .willReturn(ok(CONFIRMACION)));

        AppointmentConfirmation confirmacion =
                adapter.confirmHold("hold-6c0c2f41", "user-42", "Juan", "Perez");

        assertThat(confirmacion.reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(confirmacion.status()).isEqualTo("WAITING_FOR_PHONE");
        assertThat(confirmacion.expiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));

        String cuerpo = cuerpoDe("POST", "/api/appointment-holds/hold-6c0c2f41/confirm");
        assertThat(cuerpo).contains("\"externalPatientId\":\"user-42\"");
        assertThat(cuerpo).contains("\"patientFirstName\":\"Juan\"");
        assertThat(cuerpo).contains("\"patientLastName\":\"Perez\"");
    }

    @Test
    @DisplayName("lista reservas mandando solo los filtros que se usaron")
    void listaReservas() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointments?externalPatientId=user-42&status=CONFIRMED"
                                + "&page=0&size=20"))
                .willReturn(ok(LISTADO)));

        AppointmentsQuery query = new AppointmentsQuery(
                "user-42", CatedraReservationStatus.CONFIRMED, null, null, 0, 20);

        List<CatedraAppointment> reservas = adapter.listAppointments(query);

        assertThat(reservas).hasSize(1);
        CatedraAppointment reserva = reservas.get(0);
        assertThat(reserva.reservationId()).isEqualTo(8251L);
        assertThat(reserva.appointmentDate()).isEqualTo(DIA);
        assertThat(reserva.startTime()).isEqualTo(HORA);
        assertThat(reserva.status()).isEqualTo(CatedraReservationStatus.CONFIRMED);
        assertThat(reserva.cancelledAt()).isNull();
        assertThat(reserva.cancellationReason()).isNull();
    }

    @Test
    @DisplayName("lista sin filtros y no manda parámetros vacíos que filtrarían por vacío")
    void listaSinFiltros() {
        catedra.stubFor(get(urlEqualTo("/api/appointments?page=0&size=20"))
                .willReturn(ok("[]")));

        List<CatedraAppointment> reservas = adapter.listAppointments(AppointmentsQuery.all());

        assertThat(reservas).isEmpty();
        // Ningún parámetro vacío: externalPatientId, status, from y to no se mandan.
        assertThat(cuerpoDe("GET", "/api/appointments")).isEmpty();
    }

    @Test
    @DisplayName("cancela mandando el motivo, y el cuerpo admite motivo nulo")
    void cancela() {
        catedra.stubFor(post(urlEqualTo("/api/appointments/process-a92b47d0/cancel"))
                .willReturn(ok(CANCELACION)));

        AppointmentCancellation cancelacion =
                adapter.cancelAppointment("process-a92b47d0", "Cambio solicitado por el usuario");

        assertThat(cancelacion.reservationId()).isEqualTo(8251L);
        assertThat(cancelacion.status()).isEqualTo("CANCELLED");
        assertThat(cancelacion.cancelledAt()).isEqualTo(Instant.parse("2026-09-11T15:10:00Z"));
        assertThat(cuerpoDe("POST", "/api/appointments/process-a92b47d0/cancel"))
                .contains("\"reason\":\"Cambio solicitado por el usuario\"");
    }

    @Test
    @DisplayName("cancela sin motivo sin mandar un objeto mal formado")
    void cancelaSinMotivo() {
        catedra.stubFor(post(urlEqualTo("/api/appointments/process-a92b47d0/cancel"))
                .willReturn(ok(CANCELACION)));

        adapter.cancelAppointment("process-a92b47d0", null);

        assertThat(cuerpoDe("POST", "/api/appointments/process-a92b47d0/cancel"))
                .contains("reason");
    }

    // ------------------------------------------------------------------ autenticación

    @Test
    @DisplayName("pide el JWT técnico una sola vez y lo manda en cada llamada")
    void pideElTokenUnaVez() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .willReturn(ok(OCUPACIONES)));

        adapter.getOccupancies(15, DIA, DIA);
        adapter.getOccupancies(15, DIA, DIA);

        assertThat(cuerposDe("POST", "/api/authenticate")).hasSize(1);
        assertThat(encabezados("GET", "/api/appointment-occupancies"))
                .containsExactly("Bearer jwt-1", "Bearer jwt-1");
    }

    @Test
    @DisplayName("el login pide la vigencia larga, que es la que manda el contrato")
    void loginConVigenciaLarga() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .willReturn(ok(OCUPACIONES)));

        adapter.getOccupancies(15, DIA, DIA);

        String cuerpo = cuerpoDe("POST", "/api/authenticate");
        assertThat(cuerpo).contains("\"username\":\"juan.perez\"");
        assertThat(cuerpo).contains("\"password\":\"una-clave-segura\"");
        assertThat(cuerpo).contains("\"rememberMe\":true");
    }

    @Test
    @DisplayName("ante un 401 renueva el token y reintenta una sola vez, con el nuevo")
    void renuevaElTokenAnteUn401() {
        // Se limpia lo que dejó preparar(): esta prueba pone su propia cadena de logins,
        // y un stub genérico que compite con los de escenario haría ambiguo cuál responde.
        catedra.resetAll();

        // Cadena: login jwt-1 -> ocupaciones 401 -> login jwt-2 -> ocupaciones 200.
        catedra.stubFor(post(urlEqualTo("/api/authenticate"))
                .inScenario("renovacion")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_token\":\"jwt-1\"}"))
                .willSetStateTo("logueado"));
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .inScenario("renovacion")
                .whenScenarioStateIs("logueado")
                .willReturn(aResponse().withStatus(401)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"status\":401,\"title\":\"Unauthorized\"}"))
                .willSetStateTo("rechazado"));
        catedra.stubFor(post(urlEqualTo("/api/authenticate"))
                .inScenario("renovacion")
                .whenScenarioStateIs("rechazado")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_token\":\"jwt-2\"}"))
                .willSetStateTo("logueado-dos"));
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .inScenario("renovacion")
                .whenScenarioStateIs("logueado-dos")
                .willReturn(ok(OCUPACIONES)));

        List<Occupancy> ocupaciones = adapter.getOccupancies(15, DIA, DIA);

        assertThat(ocupaciones).hasSize(2);
        assertThat(encabezados("GET", "/api/appointment-occupancies"))
                .containsExactly("Bearer jwt-1", "Bearer jwt-2");
        assertThat(cuerposDe("POST", "/api/authenticate")).hasSize(2);
    }

    @Test
    @DisplayName("si el token recien pedido tambien se rechaza, no entra en bucle")
    void noEntraEnBucleDeRenovacion() {
        catedra.stubFor(post(urlEqualTo("/api/authenticate")).willReturn(
                ok("{\"id_token\":\"x\"}")));
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .willReturn(aResponse().withStatus(401)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"status\":401}")));

        assertThatThrownBy(() -> adapter.getOccupancies(15, DIA, DIA))
                .isInstanceOf(CatedraRejectedException.class);

        // Un login de arranque y uno por la renovación: dos, no una escalera. Y dos
        // peticiones de ocupaciones, una por intento: la renovación no es un bucle.
        assertThat(cuerposDe("POST", "/api/authenticate")).hasSize(2);
        assertThat(cuerposDe("GET", "/api/appointment-occupancies")).hasSize(2);
    }

    // ------------------------------------------------------------------ reintentos

    @Test
    @DisplayName("reintenta la lectura de ocupaciones cuando la conexión se corta")
    void reintentaLaLectura() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .inScenario("intermitente")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("cortado"));
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .inScenario("intermitente")
                .whenScenarioStateIs("cortado")
                .willReturn(ok(OCUPACIONES)));

        List<Occupancy> ocupaciones = adapter.getOccupancies(15, DIA, DIA);

        assertThat(ocupaciones).hasSize(2);
        assertThat(cuerposDe("GET", "/api/appointment-occupancies")).hasSize(2);
    }

    @Test
    @DisplayName("no reintenta crear el reten: un timeout no prueba que haya fallado")
    void noReintentaLaEscritura() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> adapter.createHold(15, DIA, HORA, "user-42"))
                .isInstanceOf(CatedraUnavailableException.class);

        // Una sola petición: repetirla podría dejar dos retenes.
        assertThat(cuerposDe("POST", "/api/appointment-holds")).hasSize(1);
    }

    @Test
    @DisplayName("no reintenta confirmar ni cancelar tampoco")
    void noReintentaLasOtrasEscrituras() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds/hold-6c0c2f41/confirm"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        catedra.stubFor(post(urlEqualTo("/api/appointments/process-a92b47d0/cancel"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> adapter.confirmHold("hold-6c0c2f41", "user-42", "Juan", "Perez"))
                .isInstanceOf(CatedraUnavailableException.class);
        assertThatThrownBy(() -> adapter.cancelAppointment("process-a92b47d0", null))
                .isInstanceOf(CatedraUnavailableException.class);

        assertThat(cuerposDe("POST", "/api/appointment-holds/hold-6c0c2f41/confirm")).hasSize(1);
        assertThat(cuerposDe("POST", "/api/appointments/process-a92b47d0/cancel")).hasSize(1);
    }

    // ------------------------------------------------------------------ errores por la red

    @Test
    @DisplayName("un HOLD_EXPIRED de la catedra llega como nuestra excepción, por la red")
    void retenVencidoDesdeLaRed() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds/hold-6c0c2f41/confirm"))
                .willReturn(aResponse().withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody(RETEN_VENCIDO)));

        assertThatThrownBy(() ->
                        adapter.confirmHold("hold-6c0c2f41", "user-42", "Juan", "Perez"))
                .isInstanceOf(HoldExpiredException.class)
                // El nombre del retén está en el mensaje: es lo que hace que la frase sirva
                // para empezar a investigar. La causa original no se adjunta en la matriz de
                // errores de negocio —ahí status + code ya dicen qué pasó—; se adjunta en los
                // casos donde cambia el diagnóstico, que son los de no disponible y rechazo.
                .hasMessageContaining("hold-6c0c2f41");
    }

    @Test
    @DisplayName("una validación de la catedra llega con sus campos y sin el objectName ajeno")
    void validacionDesdeLaRed() {
        catedra.stubFor(post(urlEqualTo("/api/appointment-holds"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody(VALIDACION)));

        assertThatThrownBy(() -> adapter.createHold(15, DIA, HORA, "user-42"))
                .isInstanceOf(CatedraValidationException.class)
                .satisfies(ex -> {
                    CatedraValidationException validacion =
                            (CatedraValidationException) ex;
                    assertThat(validacion.getFieldErrors())
                            .containsExactly("externalPatientId: size must be between 1 and 100");
                    assertThat(validacion.getMessage())
                            .doesNotContain("appointmentHoldRequestDTO");
                });
    }

    @Test
    @DisplayName("un 503 sin body es no disponible, y conserva la causa original")
    void indisponibleDesdeLaRed() {
        catedra.stubFor(get(urlEqualTo(
                        "/api/appointment-occupancies?professionalId=15"
                                + "&from=2026-09-14&to=2026-09-14"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter.getOccupancies(15, DIA, DIA))
                .isInstanceOf(CatedraUnavailableException.class)
                .satisfies(ex -> assertThat(ex.getCause()).isNotNull());
    }

    // ------------------------------------------------------------------ helpers

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder ok(
            String cuerpo) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(cuerpo);
    }

    private void stubearLogin(String token) {
        catedra.stubFor(post(urlEqualTo("/api/authenticate"))
                .willReturn(ok("{\"id_token\":\"" + token + "\"}")));
    }

    private List<ServeEvent> eventos(String metodo, String path) {
        // getAllServeEvents() devuelve lo más reciente primero; se da vuelta para que las
        // aserciones de orden —¿qué token se mandó en cada intento?— lean como sucedió.
        return catedra.getAllServeEvents().stream()
                .filter(evento -> evento.getRequest().getMethod().value().equals(metodo)
                        && evento.getRequest().getUrl().startsWith(path))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), List::reversed));
    }

    private String cuerpoDe(String metodo, String path) {
        return eventos(metodo, path).stream()
                .map(evento -> evento.getRequest().getBodyAsString())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no hubo petición " + metodo + " " + path));
    }

    private List<String> cuerposDe(String metodo, String path) {
        return eventos(metodo, path).stream()
                .map(evento -> evento.getRequest().getBodyAsString())
                .toList();
    }

    private List<String> encabezados(String metodo, String path) {
        return eventos(metodo, path).stream()
                .map(evento -> evento.getRequest().getHeader("Authorization"))
                .toList();
    }
}
