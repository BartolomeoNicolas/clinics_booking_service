package com.bartolomeo.clinics.booking.reservation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ReconcileReservationsUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationRepository;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.repository.JpaUserRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El reinicio con operaciones pendientes (enunciado §8, evidencia del anexo §18.4).
 *
 * <p><strong>Qué se prueba.</strong> El servicio se apaga con un proceso a medio hacer —está
 * esperando el teléfono— y vuelve a levantar. El evento que confirmaba la reserva se perdió
 * en el apagón, así que el proceso local no sabe lo que la cátedra ya sabe. Lo que tiene que
 * pasar es que al volver, el servicio pregunte y deje el proceso donde realmente está.
 *
 * <p><strong>Por qué contra una base y una cátedra de verdad.</strong> El proceso tiene que
 * sobrevivir al apagón, y eso solo se prueba con una base de verdad: el estado local de un
 * proceso a medio hacer no está en un objeto de Java, está en una fila. Y la pregunta a la
 * cátedra tiene que salir por HTTP con el formato que ella entiende, que es lo que un doble
 * mal armado dejaría pasar.
 */
@SpringBootTest
class ReconciliationAfterRestartIntegrationTest {

    /** El turno es mañana y el proceso vence en diez minutos: un proceso vivo. */
    private static final LocalDate DIA = LocalDate.now().plusDays(1);
    private static final LocalTime HORA = LocalTime.of(9, 30);
    private static final Instant AHORA = Instant.now();

    /** La reserva que la cátedra ya tiene confirmada, con su proceso. */
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

    private static PostgreSQLContainer postgres;
    private static WireMockServer catedra;

    @Autowired
    private ReconcileReservationsUseCase reconciliar;

    @Autowired
    private ReservationProcessRepository procesos;

    @Autowired
    private ReservationRepository reservas;

    @Autowired
    private JpaUserRepository usuarios;

    @Autowired
    private com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository
            .JpaReservationProcessRepository jpaProcesos;

    @Autowired
    private com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository
            .JpaReservationRepository jpaReservas;

    @BeforeAll
    static void levantarLosDobles() {
        postgres = new PostgreSQLContainer("postgres:16-alpine");
        postgres.start();

        catedra = new WireMockServer(wireMockConfig().dynamicPort());
        catedra.start();
    }

    @AfterAll
    static void bajarLosDobles() {
        if (catedra != null) {
            catedra.stop();
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
        registry.add("clinics.catedra.base-url", () -> catedra.baseUrl());
        registry.add("clinics.catedra.technical-user", () -> "alumno");
        registry.add("clinics.catedra.technical-password", () -> "clave-de-prueba");
        // El barrido se apaga: acá se prueba la reconciliación, no la tarea programada.
        registry.add("clinics.reservation.sweep-interval-ms", () -> "3600000");
    }

    @BeforeEach
    void preparar() {
        catedra.resetAll();

        // La cátedra: el token técnico y el listado de reservas.
        catedra.stubFor(post(urlEqualTo("/api/authenticate"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_token\":\"jwt-1\"}")));
        catedra.stubFor(get(urlPathEqualTo("/api/appointments"))
                .willReturn(okJson(LISTADO)));

        // El orden importa: las reservas referencian a los procesos y estos al usuario.
        jpaReservas.deleteAll();
        jpaProcesos.deleteAll();
        usuarios.deleteAll();
    }

    private Long creaUsuario() {
        UserEntity usuario = UserEntity.builder()
                .login("nbartolomeo")
                .passwordHash("hash")
                .firstName("Nicolás")
                .lastName("Bartolomeo")
                .email("nico@ejemplo.edu")
                .langKey("es")
                .activated(true)
                .authorities(Set.of())
                .build();
        return usuarios.save(usuario).getId();
    }

    /**
     * Un proceso que quedó esperando el teléfono: la reserva se confirmó en la cátedra, pero
     * el evento que lo decía se perdió en el apagón.
     */
    private Long dejaUnProcesoAMedias(Long userId) {
        ReservationProcess proceso = new ReservationProcess(
                null, userId, "user-42", 15L, DIA, HORA,
                ReservationState.WAITING_FOR_PHONE,
                "hold-6c0c2f41", "process-a92b47d0", "request-event-8a31",
                AHORA.plusSeconds(600), "clave-1", null, AHORA, AHORA);

        return procesos.save(proceso).id();
    }

    @Test
    @DisplayName("tras un reinicio el proceso converge con el estado real de la catedra")
    void trasUnReinicioElProcesoConverge() {
        Long userId = creaUsuario();
        Long processId = dejaUnProcesoAMedias(userId);

        int convergidos = reconciliar.reconcilePending();

        assertThat(convergidos).isEqualTo(1);

        ReservationProcess despues = procesos.findById(processId).orElseThrow();
        assertThat(despues.state()).isEqualTo(ReservationState.CONFIRMED);
    }

    @Test
    @DisplayName("la convergencia deja la reserva guardada, que es el turno tomado")
    void laConvergenciaDejaLaReservaGuardada() {
        Long userId = creaUsuario();
        Long processId = dejaUnProcesoAMedias(userId);

        reconciliar.reconcilePending();

        assertThat(reservas.findByProcessId(processId)).isPresent();
        assertThat(reservas.findByProcessId(processId).orElseThrow().catedraReservationId())
                .isEqualTo(8251L);
    }
}
