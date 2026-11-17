package com.bartolomeo.clinics.booking.catalog.infrastructure.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bartolomeo.clinics.booking.catalog.application.exception.CatalogRejectedException;
import com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException;
import com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException;
import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.ProfessionalCategory;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import com.bartolomeo.clinics.booking.catalog.infrastructure.config.CatalogProperties;
import com.bartolomeo.clinics.booking.catalog.infrastructure.security.ServiceTokenIssuer;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
import com.bartolomeo.clinics.booking.shared.infrastructure.security.JwtProperties;
import com.bartolomeo.clinics.booking.user.domain.model.Role;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.infrastructure.security.JwtTokenIssuerAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * El contrato de booking con el servicio de catálogo, pegado al cliente real.
 *
 * <p><strong>Las fixtures no las escribimos: se capturaron de las respuestas reales.</strong>
 * El JSON de este archivo salió de correr el catálogo de verdad —los DTO y el serializador
 * del otro servicio— y pegar lo que devolvió cada endpoint. Eso es lo que hace que este test
 * mida algo: no valida que booking sepa leer lo que nosotros imaginamos que el otro manda,
 * sino que sepa leer lo que manda.
 *
 * <p><strong>No se mockea nuestro propio código.</strong> El adaptador es el de producción, con
 * su {@code RestClient}, sus timeouts y su emisor de token reales; el único doble es el
 * servidor HTTP del otro lado, que es justamente la frontera. Si el test pasara con un adapter
 * inventado, no estaría probando el contrato sino nuestra idea del contrato.
 *
 * <p><strong>Cuándo se vuelve a tocar.</strong> Si el catálogo cambia la forma de sus
 * respuestas, hay que volver a capturarlas y reemplazar este JSON: entonces el test se pone
 * rojo —falta un campo, cambió un tipo— y obliga a arreglar el mapper antes de que llegue a
 * producción. Ese es el mecanismo completo de "el test falla si el contrato cambia de forma
 * incompatible", y depende de una convención que se cumple a mano: los fixtures no se editan
 * para que pasen, se recapturan.
 */
@DisplayName("el contrato del catálogo contra las respuestas reales del servicio")
class CatalogQueryContractTest {

    // ------------------------------------------------------------------ fixtures

    /** Capturada de GET /api/catalog/categories. */
    private static final String CATEGORIAS = """
            [{"id":1,"name":"Clinica medica","description":null,"enabled":true,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z"},\
            {"id":3,"name":"Dermatologia","description":null,"enabled":false,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z"},\
            {"id":2,"name":"Kinesiologia","description":null,"enabled":true,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z"}]\
            """;

    /** Capturada de GET /api/catalog/professionals/15. */
    private static final String FICHA = """
            {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z",\
            "weeklySchedules":[{"id":21,"professionalId":15,"dayOfWeek":"MONDAY",\
            "startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,\
            "enabled":true,"createdAt":"2026-07-01T12:00:00Z",\
            "updatedAt":"2026-11-02T10:00:00Z"}]}\
            """;

    /** Capturada de GET /api/catalog/professionals/16/weekly-schedules. */
    private static final String AGENDA = """
            [{"id":22,"professionalId":16,"dayOfWeek":"MONDAY","startTime":"14:00:00",\
            "endTime":"18:00:00","slotDurationMinutes":60,"enabled":true,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z"},\
            {"id":23,"professionalId":16,"dayOfWeek":"TUESDAY","startTime":"09:00:00",\
            "endTime":"12:00:00","slotDurationMinutes":30,"enabled":false,\
            "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-11-02T10:00:00Z"}]\
            """;

    /** Capturada de GET /api/catalog/professionals/999, con su problem+json. */
    private static final String NO_ENCONTRADO = """
            {"type":"https://dqmdz-um.github.io/prog2/clinics/problems/not-found",\
            "title":"No encontrado","status":404,\
            "detail":"No existe el profesional 999 en el catalogo local.",\
            "path":"/api/catalog/professionals/999","message":"No encontrado",\
            "code":"not-found","timestamp":"2026-11-03T14:23:11.598412279Z"}\
            """;

    private static WireMockServer catalogo;

    private CatalogRestAdapter adapter;
    private CatalogProperties properties;
    private JwtProperties jwtProperties;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeAll
    static void levantarElServidorFalso() {
        catalogo = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        catalogo.start();
    }

    @AfterAll
    static void bajarElServidorFalso() {
        catalogo.stop();
    }

    @BeforeEach
    void preparar() {
        catalogo.resetAll();
        SecurityContextHolder.clearContext();

        KeyPair par = generarParRsa();
        encoder = NimbusJwtEncoder
                .withKeyPair((RSAPublicKey) par.getPublic(), (RSAPrivateKey) par.getPrivate())
                .build();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) par.getPublic()).build();

        jwtProperties = new JwtProperties();
        jwtProperties.setAudience("clinics");
        jwtProperties.setIssuer("clinics-booking");

        properties = new CatalogProperties();
        properties.setBaseUrl(catalogo.baseUrl());
        properties.setServiceName("clinics-booking");

        adapter = crearAdapter(properties);
    }

    @AfterEach
    void limpiarElContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------- lectura de las tres rutas

    @Test
    @DisplayName("lee las categorias respetando el nulo y el orden del catálogo")
    void leeLasCategorias() {
        stubear("/api/catalog/categories", 200, CATEGORIAS);

        List<ProfessionalCategory> categorias = adapter.getCategories();

        assertThat(categorias).extracting(ProfessionalCategory::name)
                .containsExactly("Clinica medica", "Dermatologia", "Kinesiologia");
        assertThat(categorias).extracting(ProfessionalCategory::id)
                .containsExactly(1L, 3L, 2L);
        assertThat(categorias.get(0).description()).isNull();
        assertThat(categorias.get(1).enabled()).isFalse();
        assertThat(categorias.get(2).enabled()).isTrue();
    }

    @Test
    @DisplayName("lee la ficha de un profesional dejando pasar lo que no usa")
    void leeLaFicha() {
        stubear("/api/catalog/professionals/15", 200, FICHA);

        Optional<Professional> profesional = adapter.getProfessional(15);

        // En la respuesta hay createdAt, updatedAt y un arreglo entero de horarios, y ninguno
        // esta en el modelo: se ignoraron en vez de romper la lectura.
        assertThat(profesional).isPresent();
        assertThat(profesional.get())
                .satisfies(p -> {
                    assertThat(p.id()).isEqualTo(15L);
                    assertThat(p.categoryId()).isEqualTo(1L);
                    assertThat(p.firstName()).isEqualTo("Ana");
                    assertThat(p.lastName()).isEqualTo("Gomez");
                    assertThat(p.enabled()).isTrue();
                });
    }

    @Test
    @DisplayName("lee la agenda traduciendo el dia de texto a enum y las horas a LocalTime")
    void leeLaAgenda() {
        stubear("/api/catalog/professionals/16/weekly-schedules", 200, AGENDA);

        List<WeeklySchedule> agenda = adapter.getWeeklySchedules(16);

        assertThat(agenda).hasSize(2);
        assertThat(agenda.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(agenda.get(0).startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(agenda.get(0).endTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(agenda.get(0).slotDurationMinutes()).isEqualTo(60);
        assertThat(agenda.get(0).enabled()).isTrue();
        // La regla apagada tambien viaja: quien la descarta es turnos, no el cliente.
        assertThat(agenda.get(1).dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(agenda.get(1).enabled()).isFalse();
    }

    @Test
    @DisplayName("tolera un campo que se agrega del otro lado sin avisar")
    void toleraCamposNuevos() {
        String conCamposNuevos = FICHA
                .replace("\"enabled\":true",
                        "\"enabled\":true,\"avatarUrl\":\"https://cdn/x.png\","
                                + "\"revision\":7,\"suscribir\":false");
        stubear("/api/catalog/professionals/15", 200, conCamposNuevos);

        Optional<Professional> profesional = adapter.getProfessional(15);

        assertThat(profesional).isPresent();
        assertThat(profesional.get().firstName()).isEqualTo("Ana");
    }

    // ------------------------------------------------------------- estados de error

    @Test
    @DisplayName("la ficha inexistente queda vacia y no un error")
    void fichaInexistente() {
        stubear("/api/catalog/professionals/999", 404, NO_ENCONTRADO);

        assertThat(adapter.getProfessional(999)).isEmpty();
    }

    @Test
    @DisplayName("la agenda de alguien que no existe es un error, no una lista vacía")
    void agendaDeProfesionalInexistente() {
        stubear("/api/catalog/professionals/999/weekly-schedules", 404, NO_ENCONTRADO);

        assertThatThrownBy(() -> adapter.getWeeklySchedules(999))
                .isInstanceOfSatisfying(ProfessionalNotFoundException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("que no existan categorias es una falla de integración, no un 404 propio")
    void categoriasInexistentes() {
        stubear("/api/catalog/categories", 404, NO_ENCONTRADO);

        assertThatThrownBy(() -> adapter.getCategories())
                .isInstanceOfSatisfying(CatalogUnavailableException.class, ex ->
                        assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.CATALOG_UNAVAILABLE));
    }

    @Test
    @DisplayName("un 401 del catálogo se traduce como credencial rechazada y no como 401 del usuario")
    void rechazoPor401() {
        stubear("/api/catalog/categories", 401, "{}");

        assertThatThrownBy(() -> adapter.getCategories())
                .isInstanceOfSatisfying(CatalogRejectedException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATALOG_REJECTED);
                    assertThat(ex.getCause()).isNotNull();
                });
    }

    @Test
    @DisplayName("un 403 del catálogo se traduce igual que un 401")
    void rechazoPor403() {
        stubear("/api/catalog/categories", 403, "{}");

        assertThatThrownBy(() -> adapter.getCategories())
                .isInstanceOf(CatalogRejectedException.class);
    }

    @Test
    @DisplayName("un 500 del catálogo se traduce como catálogo no disponible")
    void falloDelServidor() {
        stubear("/api/catalog/categories", 500, "boom");

        assertThatThrownBy(() -> adapter.getCategories())
                .isInstanceOfSatisfying(CatalogUnavailableException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATALOG_UNAVAILABLE);
                    assertThat(ex.getCause()).isNotNull();
                });
    }

    @Test
    @DisplayName("un 400 inesperado tambien queda como falla de integración")
    void respuesta400() {
        stubear("/api/catalog/categories", 400, "{}");

        assertThatThrownBy(() -> adapter.getCategories())
                .isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    @DisplayName("sin nadie escuchando se traduce como catálogo no disponible y no revienta")
    void sinConexion() throws IOException {
        int puertoMuerto = puertoLibreCerrado();
        CatalogProperties ahi = new CatalogProperties();
        ahi.setBaseUrl("http://localhost:" + puertoMuerto);
        ahi.setServiceName("clinics-booking");
        CatalogRestAdapter sinConexion = crearAdapter(ahi);

        assertThatThrownBy(() -> sinConexion.getCategories())
                .isInstanceOfSatisfying(CatalogUnavailableException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATALOG_UNAVAILABLE));
    }

    @Test
    @DisplayName("una respuesta que no es el contrato falla en lugar de devolver basura")
    void contratoRoto() {
        // El arreglo sigue siendo un arreglo y el resto del JSON sigue igual: cambia solo el
        // tipo de un campo. Un cliente que pasara de largo este error estaria devolviendo una
        // hora inventada en vez de un error, y esa es la manera mas cara de enterarse.
        String roto = AGENDA.replace("\"startTime\":\"14:00:00\"",
                "\"startTime\":{\"hora\":9}");
        stubear("/api/catalog/professionals/16/weekly-schedules", 200, roto);

        assertThatThrownBy(() -> adapter.getWeeklySchedules(16))
                .isInstanceOf(CatalogUnavailableException.class);
    }

    // ------------------------------------------------------------ el JWT técnico

    @Test
    @DisplayName("llama siempre con un JWT técnico que lleva la identidad de la sesión")
    void mandaElJwtTecnico() {
        autenticarComoUsuarioDeId15();
        stubear("/api/catalog/categories", 200, CATEGORIAS);

        adapter.getCategories();

        List<ServeEvent> pedidos = catalogo.getAllServeEvents();
        assertThat(pedidos).as("una llamada por consulta").hasSize(1);

        String encabezado = pedidos.get(0).getRequest().getHeader("Authorization");
        assertThat(encabezado).startsWith("Bearer ");
        Jwt tecnico = decoder.decode(encabezado.substring("Bearer ".length()));

        // sub = servicio, y no el usuario: quien llama es el backend.
        assertThat(tecnico.getSubject()).isEqualTo("clinics-booking");
        // Se lee como texto y no con getIssuer(), que interpreta el claim como una URL y
        // "clinics-booking" no lo es: el issuer de este servicio es un nombre, no una URI.
        assertThat(tecnico.getClaimAsString("iss")).isEqualTo("clinics-booking");
        assertThat(tecnico.getAudience()).containsExactly("clinics");

        // La identidad viaja adentro del token firmado, no en un parametro de la URL.
        assertThat(tecnico.getClaimAsString("login")).isEqualTo("nbartolomeo");
        Object userId = tecnico.getClaim("userId");
        assertThat(String.valueOf(userId)).isEqualTo("15");
        assertThat(tecnico.getClaimAsStringList("scope")).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("si no hay sesión, el token sale solo con la identidad del servicio")
    void sinSesionNoInventaIdentidad() {
        stubear("/api/catalog/categories", 200, CATEGORIAS);

        adapter.getCategories();

        String encabezado = catalogo.getAllServeEvents().get(0).getRequest()
                .getHeader("Authorization");
        Jwt tecnico = decoder.decode(encabezado.substring("Bearer ".length()));

        assertThat(tecnico.getSubject()).isEqualTo("clinics-booking");
        Object login = tecnico.getClaim("login");
        Object userId = tecnico.getClaim("userId");
        assertThat(login).isNull();
        assertThat(userId).isNull();
    }

    // ------------------------------------------------------------------ herramientas

    private CatalogRestAdapter crearAdapter(CatalogProperties propiedades) {
        ServiceTokenIssuer emisor = new ServiceTokenIssuer(encoder, jwtProperties, propiedades);
        CatalogRestAdapter creado = new CatalogRestAdapter(propiedades, emisor);
        creado.buildClient();
        return creado;
    }

    /**
     * Deja un token de sesión real en el contexto de seguridad, como lo dejaría el resource
     * server en una petición autenticada.
     *
     * <p>Se emite con el emisor de sesión de producción y no se inventa un JWT a mano, porque
     * lo que se quiere probar es justamente la cadena completa: que el id que este servicio
     * puso en su token de sesión llega al token técnico que le manda al catálogo.
     */
    private void autenticarComoUsuarioDeId15() {
        JwtTokenIssuerAdapter emisorDeSesion = new JwtTokenIssuerAdapter(encoder, jwtProperties);
        User usuario = User.builder()
                .id(15L)
                .login("nbartolomeo")
                .roles(List.of(Role.ROLE_USER))
                .build();
        String tokenDeSesion = emisorDeSesion.issue(usuario).token();

        Jwt sesion = decoder.decode(tokenDeSesion);
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(sesion));
    }

    private void stubear(String ruta, int status, String cuerpo) {
        catalogo.stubFor(get(urlEqualTo(ruta))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", status == 404
                                ? "application/problem+json"
                                : "application/json")
                        .withBody(cuerpo)));
    }

    private static KeyPair generarParRsa() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK sin RSA", ex);
        }
    }

    /** Un puerto que nadie esta escuchando, para probar la caida del catálogo. */
    private static int puertoLibreCerrado() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
