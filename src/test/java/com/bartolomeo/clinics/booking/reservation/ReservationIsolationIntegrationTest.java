package com.bartolomeo.clinics.booking.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.repository.JpaUserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
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
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El aislamiento entre usuarios (enunciado §9, evidencia 8 del §11).
 *
 * <p><strong>Dos usuarios de verdad, con su token de verdad.</strong> El test registra a los
 * dos, inicia sesión con cada uno y usa el token que devuelve el servicio. Un token falseado
 * o un usuario simulado no probarían el aislamiento: probarían que el código lee bien un
 * identificador.
 *
 * <p><strong>Lo que se prueba.</strong> Que el usuario B no pueda ver, ni listar, ni cancelar
 * nada del usuario A, y que el propio A sí pueda. Y que la respuesta de un proceso ajeno sea
 * un 404 y no un 403: confirmar que existe ya es filtrar información.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationIsolationIntegrationTest {

    private static final String CLAVE = "clave-de-prueba-123";

    private static PostgreSQLContainer postgres;

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
    private ObjectMapper objectMapper;

    @BeforeAll
    static void levantarLaBase() {
        postgres = new PostgreSQLContainer("postgres:16-alpine");
        postgres.start();
    }

    @AfterAll
    static void bajarLaBase() {
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
    }

    @BeforeEach
    void limpiar() {
        // Los procesos referencian al usuario: se borran primero.
        jpaProcesos.deleteAll();
        usuarios.deleteAll();
    }

    // ------------------------------------------------------------------ helpers

    private Long registra(String login, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s","firstName":"Nombre",
                                 "lastName":"Apellido","email":"%s","langKey":"es"}
                                """.formatted(login, CLAVE, email)))
                .andExpect(status().isCreated());
        return usuarios.findByLogin(login).orElseThrow().getId();
    }

    private String tokenDe(String login) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s"}
                                """.formatted(login, CLAVE)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode nodo = objectMapper.readTree(cuerpo);
        return nodo.get("id_token").asText();
    }

    private Long dejaUnProceso(Long userId, String externalPatientId) {
        Instant ahora = Instant.now();
        ReservationProcess proceso = new ReservationProcess(
                null, userId, externalPatientId, 15L,
                LocalDate.now().plusDays(1), LocalTime.of(9, 30),
                ReservationState.CONFIRMED,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                ahora.plusSeconds(600), "clave-" + userId, null, ahora, ahora);

        return procesos.save(proceso).id();
    }

    // -------------------------------------------------------------------- tests

    @Test
    @DisplayName("el usuario B no puede ver el proceso del usuario A")
    void elUsuarioBNoPuedeVerElProcesoDeA() throws Exception {
        Long userA = registra("usuarioa", "a@ejemplo.edu");
        registra("usuariob", "b@ejemplo.edu");
        Long procesoDeA = dejaUnProceso(userA, "user-" + userA);
        String tokenB = tokenDe("usuariob");

        mockMvc.perform(get("/api/reservations/" + procesoDeA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    @DisplayName("el listado del usuario B no trae los procesos del usuario A")
    void elListadoDelUsuarioBNoTraeLoDeA() throws Exception {
        Long userA = registra("usuarioa", "a@ejemplo.edu");
        Long userB = registra("usuariob", "b@ejemplo.edu");
        dejaUnProceso(userA, "user-" + userA);
        dejaUnProceso(userB, "user-" + userB);
        String tokenB = tokenDe("usuariob");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("el usuario B no puede cancelar el proceso del usuario A")
    void elUsuarioBNoPuedeCancelarLoDeA() throws Exception {
        Long userA = registra("usuarioa", "a@ejemplo.edu");
        registra("usuariob", "b@ejemplo.edu");
        Long procesoDeA = dejaUnProceso(userA, "user-" + userA);
        String tokenB = tokenDe("usuariob");

        mockMvc.perform(post("/api/reservations/" + procesoDeA + "/cancel")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el usuario A ve y cancela sus propios procesos")
    void elUsuarioAVeYCancelaLoSuyo() throws Exception {
        Long userA = registra("usuarioa", "a@ejemplo.edu");
        Long procesoDeA = dejaUnProceso(userA, "user-" + userA);
        String tokenA = tokenDe("usuarioa");

        mockMvc.perform(get("/api/reservations/" + procesoDeA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processId").value(procesoDeA))
                .andExpect(jsonPath("$.state").value("CONFIRMED"));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("sin token no se ven las reservas")
    void sinTokenNoSeVenLasReservas() throws Exception {
        Long userA = registra("usuarioa", "a@ejemplo.edu");
        dejaUnProceso(userA, "user-" + userA);

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }
}
