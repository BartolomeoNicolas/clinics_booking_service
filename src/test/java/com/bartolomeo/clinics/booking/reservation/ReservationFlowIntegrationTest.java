package com.bartolomeo.clinics.booking.reservation;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.repository.JpaUserRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El camino completo de una reserva, contra las tres dependencias de verdad: PostgreSQL,
 * Kafka y la cátedra (un doble de HTTP).
 *
 * <p><strong>Por qué Kafka de verdad y no un doble del listener.</strong> Lo que hay que
 * probar del consumidor es cuándo confirma el offset y qué pasa cuando el mismo mensaje
 * llega dos veces. Eso es comportamiento del contenedor de Kafka, no de una clase nuestra:
 * un doble probaría que llamamos al método que queríamos llamar.
 *
 * <p><strong>Por qué la cátedra es un doble.</strong> El contrato con la cátedra ya está
 * probado con sus fixtures literales en {@code CatedraAppointmentsContractTest}. Acá lo que
 * importa es cómo reacciona este servicio a lo que ella devuelve.
 *
 * <p><strong>Los escenarios son los que importan.</strong> La reserva confirmada completa,
 * la que se vence sin que el usuario mande el teléfono, el rechazo reintentable, el mensaje
 * duplicado y el evento fuera de orden. Y el aislamiento entre usuarios y el acceso sin
 * token, que están en {@code ReservationIsolationIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationFlowIntegrationTest {

    private static final String TOPIC_ACCIONES = "alumnos.turnos.acciones.test-group";
    private static final String TOPIC_TELEFONO = "catedra.turnos.telefono.test-group";
    private static final String CLAVE = "clave-de-prueba-123";

    private static final String OCUPACIONES = """
            {"professionalId":15,"from":"2026-09-14","to":"2026-09-14","occupancies":[]}""";

    private static final String RETEN = """
            {"holdId":"hold-6c0c2f41","reservationProcessId":"process-a92b47d0",
             "status":"HELD","expiresAt":"2026-09-10T14:44:50Z"}""";

    private static final String CONFIRMACION = """
            {"reservationProcessId":"process-a92b47d0","status":"WAITING_FOR_PHONE",
             "expiresAt":"2026-09-10T14:44:50Z"}""";

    private static PostgreSQLContainer postgres;
    private static KafkaContainer kafka;
    private static WireMockServer catedra;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository usuarios;

    @Autowired
    private ReservationProcessRepository procesos;

    @Autowired
    private com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository
            .JpaReservationProcessRepository jpaProcesos;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository
            .JpaReservationRepository jpaReservas;

    @Autowired
    private com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.mapper
            .ReservationProcessMapper mapperDeProcesos;

    @BeforeAll
    static void levantarLosDobles() {
        postgres = new PostgreSQLContainer("postgres:16-alpine");
        postgres.start();

        kafka = new KafkaContainer("apache/kafka:3.9.1");
        kafka.start();

        catedra = new WireMockServer(wireMockConfig().dynamicPort());
        catedra.start();
    }

    @AfterAll
    static void bajarLosDobles() {
        if (catedra != null) {
            catedra.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("clinics.local.db-password", () -> postgres.getPassword());
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log",
                () -> "classpath:/db/changelog/db.changelog-master.xml");

        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
        registry.add("clinics.catedra.base-url", () -> catedra.baseUrl());
        registry.add("clinics.catedra.technical-user", () -> "alumno");
        registry.add("clinics.catedra.technical-password", () -> "clave-de-prueba");
        registry.add("clinics.catedra.kafka.enabled", () -> "true");
        registry.add("clinics.catedra.kafka.actions-topic", () -> TOPIC_ACCIONES);
        registry.add("clinics.catedra.kafka.phone-topic", () -> TOPIC_TELEFONO);
        registry.add("clinics.catedra.kafka.consumer-group", () -> "clinics-booking-test");
        // El barrido no corre: acá manda el evento.
        registry.add("clinics.reservation.sweep-interval-ms", () -> "3600000");
    }

    @BeforeEach
    void preparar() {
        catedra.resetAll();

        catedra.stubFor(WireMock.post(urlEqualTo("/api/authenticate"))
                .willReturn(okJson("{\"id_token\":\"jwt-1\"}")));
        catedra.stubFor(WireMock.get(urlPathEqualTo("/api/appointment-occupancies"))
                .willReturn(okJson(OCUPACIONES)));
        // El vencimiento va en el futuro: el de los fixtures del contrato ya paso y un
        // proceso vencido no admite mandarle el telefono.
        String vence = Instant.now().plusSeconds(600).toString();
        catedra.stubFor(WireMock.post(urlEqualTo("/api/appointment-holds"))
                .willReturn(okJson(RETEN.replace("2026-09-10T14:44:50Z", vence))));
        catedra.stubFor(WireMock.post(urlEqualTo("/api/appointment-holds/hold-6c0c2f41/confirm"))
                .willReturn(okJson(CONFIRMACION.replace("2026-09-10T14:44:50Z", vence))));

        // Las reservas referencian a los procesos y estos al usuario: en ese orden.
        jpaReservas.deleteAll();
        jpaProcesos.deleteAll();
        usuarios.deleteAll();
    }

    // ------------------------------------------------------------------ helpers

    private String registraYEntra() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nicobartolomeo","password":"%s","firstName":"Nicolás",
                                 "lastName":"Bartolomeo","email":"nico@ejemplo.edu","langKey":"es"}
                                """.formatted(CLAVE)))
                .andExpect(status().isCreated());

        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nicobartolomeo","password":"%s"}
                                """.formatted(CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(cuerpo).get("id_token").asText();
    }

    private long iniciaUnaReserva(String token) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "clave-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":15,"date":"%s","startTime":"09:30:00"}
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(cuerpo).get("processId").asLong();
    }

    private void publica(String topic, String cuerpo) {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, "process-a92b47d0", cuerpo)).get();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo publicar el evento", ex);
        }
    }

    private List<String> loPublicadoEn(String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("group.id", "test-lector-" + System.nanoTime());
        props.put("auto.offset.reset", "earliest");

        List<String> recibidos = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long limite = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < limite && recibidos.isEmpty()) {
                ConsumerRecords<String, String> registros = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> registro : registros) {
                    recibidos.add(registro.value());
                }
            }
        }
        return recibidos;
    }

    /** Espera a que el proceso llegue al estado esperado: el consumidor es asíncrono. */
    private ReservationProcess esperaA(ReservationState estado) throws InterruptedException {
        for (int intento = 0; intento < 100; intento++) {
            ReservationProcess proceso = jpaProcesos.findAll().stream()
                    .map(mapperDeProcesos::toDomain)
                    .filter(p -> p.state() == estado)
                    .findFirst()
                    .orElse(null);
            if (proceso != null) {
                return proceso;
            }
            Thread.sleep(100);
        }
        return null;
    }

    /** Un identificador de evento único: el dedup por {@code eventId} es real. */
    private static String eventId(String nombre) {
        return nombre + "-" + System.nanoTime();
    }

    private static String evento(String eventId, String tipo, String extra) {
        return """
                {"eventId":"%s","eventType":"%s","reservationProcessId":"process-a92b47d0",
                 "holdId":"hold-6c0c2f41","requestEventId":"request-event-8a31",
                 "occurredAt":"2026-09-10T14:35:10Z","schemaVersion":1%s}
                """.formatted(eventId, tipo, extra == null ? "" : "," + extra);
    }

    // -------------------------------------------------------------------- tests

    @Test
    @DisplayName("el camino completo: se toma el turno, se manda el telefono y se confirma")
    void elCaminoCompleto() throws Exception {
        String token = registraYEntra();

        long processId = iniciaUnaReserva(token);
        assertThat(processId).isPositive();

        // La catedra pide el telefono.
        String pedido = eventId("request");
        publica(TOPIC_ACCIONES, evento(pedido, "AdditionalInformationRequested",
                "\"requiredField\":\"PHONE_NUMBER\""));
        assertThat(esperaA(ReservationState.WAITING_FOR_PHONE)).isNotNull();

        // El usuario manda el telefono y se publica la respuesta.
        mockMvc.perform(post("/api/reservations/" + processId + "/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+54 9 261-555-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PHONE_SUBMITTED"));

        List<String> publicados = loPublicadoEn(TOPIC_TELEFONO);
        assertThat(publicados).hasSize(1);
        assertThat(publicados.getFirst()).contains("AdditionalInformationSubmitted");
        assertThat(publicados.getFirst()).contains("+5492615551234");
        // La respuesta referencia el pedido al que contesta.
        assertThat(publicados.getFirst()).contains(pedido);

        // Y la reserva queda confirmada.
        publica(TOPIC_ACCIONES, evento(eventId("confirmacion"), "AppointmentConfirmed",
                "\"reservationId\":8251,\"professionalId\":15,\"date\":\"2026-09-14\","
                        + "\"startTime\":\"09:30\",\"status\":\"CONFIRMED\""));
        assertThat(esperaA(ReservationState.CONFIRMED)).isNotNull();

        mockMvc.perform(get("/api/reservations/" + processId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONFIRMED"));
    }

    @Test
    @DisplayName("un proceso que se vence sin completar la informacion queda vencido")
    void elProcesoSeVence() throws Exception {
        String token = registraYEntra();
        long processId = iniciaUnaReserva(token);

        publica(TOPIC_ACCIONES, evento(eventId("expirado"), "AppointmentProcessExpired",
                "\"reason\":\"HOLD_EXPIRED\""));

        ReservationProcess vencido = esperaA(ReservationState.EXPIRED);
        assertThat(vencido).isNotNull();
        assertThat(vencido.id()).isEqualTo(processId);
    }

    @Test
    @DisplayName("un rechazo deja el proceso reintentable")
    void elRechazoDejaElProcesoReintentable() throws Exception {
        String token = registraYEntra();
        long processId = iniciaUnaReserva(token);

        // Primero la catedra pide el telefono: ese evento es el que hay que responder
        // despues, y el que deja guardado a que pedido responde el telefono.
        publica(TOPIC_ACCIONES, evento(eventId("request"), "AdditionalInformationRequested",
                "\"requiredField\":\"PHONE_NUMBER\""));
        assertThat(esperaA(ReservationState.WAITING_FOR_PHONE)).isNotNull();

        publica(TOPIC_ACCIONES, evento(eventId("rejection"), "AdditionalInformationRejected",
                "\"reason\":\"INVALID_PHONE_NUMBER\",\"expiresAt\":\"2026-09-10T14:44:50Z\""));

        assertThat(esperaA(ReservationState.REJECTED)).isNotNull();

        // Rechazado y reintentable: se puede mandar el telefono otra vez.
        mockMvc.perform(post("/api/reservations/" + processId + "/phone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+5492615551234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PHONE_SUBMITTED"));
    }

    @Test
    @DisplayName("el mismo evento dos veces no repite el efecto")
    void elMismoEventoDosVecesNoRepiteElEfecto() throws Exception {
        String token = registraYEntra();
        iniciaUnaReserva(token);

        String cuerpo = evento(eventId("duplicado"), "AdditionalInformationRequested",
                "\"requiredField\":\"PHONE_NUMBER\"");
        publica(TOPIC_ACCIONES, cuerpo);
        assertThat(esperaA(ReservationState.WAITING_FOR_PHONE)).isNotNull();

        // El mismo aviso, otra vez: el estado no cambia y el efecto no se repite.
        publica(TOPIC_ACCIONES, cuerpo);
        Thread.sleep(1500);

        assertThat(jpaProcesos.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("un evento fuera de orden no reabre un proceso que ya estaba cerrado")
    void unEventoFueraDeOrdenNoReabreUnProcesoCerrado() throws Exception {
        String token = registraYEntra();
        long processId = iniciaUnaReserva(token);

        // El proceso se vence primero.
        publica(TOPIC_ACCIONES, evento(eventId("expirado"), "AppointmentProcessExpired",
                "\"reason\":\"HOLD_EXPIRED\""));
        assertThat(esperaA(ReservationState.EXPIRED)).isNotNull();

        // Y despues llega la confirmacion, tarde. Un proceso cerrado no se reabre.
        publica(TOPIC_ACCIONES, evento(eventId("confirmacion-tardia"), "AppointmentConfirmed",
                "\"reservationId\":8251,\"professionalId\":15,\"date\":\"2026-09-14\","
                        + "\"startTime\":\"09:30\",\"status\":\"CONFIRMED\""));
        Thread.sleep(1500);

        mockMvc.perform(get("/api/reservations/" + processId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXPIRED"));
    }

    @Test
    @DisplayName("sin token no se puede iniciar una reserva")
    void sinTokenNoSePuedeIniciarUnaReserva() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":15,"date":"2026-09-14","startTime":"09:30:00"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
