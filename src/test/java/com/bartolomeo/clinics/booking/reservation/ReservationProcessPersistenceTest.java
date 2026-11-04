package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bartolomeo.clinics.booking.reservation.application.exception.DuplicateReservationRequestException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationState;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.ReservationProcessRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.bartolomeo.clinics.booking.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.user.infrastructure.persistence.repository.JpaUserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El proceso de reserva contra una base real.
 *
 * <p>El contenedor levanta el esquema con el mismo changelog de Liquibase que usa el
 * servicio, así que el test ejercita el esquema también: si el CHECK de estados o el índice
 * único no estuvieran como deben, la aplicación ni siquiera levantaría.
 *
 * <p><strong>Lo que se prueba acá es lo que los mocks no pueden probar.</strong> Que el
 * estado se guarde como texto y vuelva como enumeración, que el vencimiento con zona
 * horaria vuelva igual, y sobre todo que la clave de idempotencia sea única por usuario: esa
 * garantía no está en el código sino en el índice, y un índice que no está no se nota hasta
 * que dos peticiones llegan a la vez.
 */
@SpringBootTest
@Testcontainers
class ReservationProcessPersistenceTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("clinics_booking")
            .withUsername("booking_app")
            .withPassword("clave-de-prueba");

    private static final Path KEY_DIR = tempKeyDir();

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final LocalTime HORA = LocalTime.of(9, 30);
    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");
    private static final Instant VENCE = AHORA.plusSeconds(600);

    @Autowired
    private ReservationProcessRepository repository;

    @Autowired
    private JpaReservationProcessRepository jpaRepository;

    @Autowired
    private JpaUserRepository usuarios;

    private static Path tempKeyDir() {
        try {
            Path dir = Files.createTempDirectory("booking-jwt-");
            dir.toFile().deleteOnExit();
            return dir;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear el directorio temporal de claves", ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("clinics.local.db-password", POSTGRES::getPassword);
        registry.add("clinics.security.jwt.private-key-path",
                () -> KEY_DIR.resolve("jwt-private.pem").toString());
        registry.add("clinics.security.jwt.public-key-path",
                () -> KEY_DIR.resolve("jwt-public.pem").toString());
        registry.add("clinics.security.jwt.audience", () -> "clinics");
        registry.add("clinics.security.jwt.issuer", () -> "clinics-booking");
        // El consumidor de Kafka se apaga: estos tests no necesitan broker y sin él el
        // contenedor solo produce ruido de conexion.
        registry.add("clinics.catedra.kafka.enabled", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @BeforeEach
    void limpia() {
        jpaRepository.deleteAll();
        usuarios.deleteAll();
    }

    private Long creaUsuario() {
        UserEntity usuario = UserEntity.builder()
                .login("nicobartok0")
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

    private ReservationProcess proceso(Long userId, String requestKey) {
        return ReservationProcess.initiated(
                userId, "user-" + userId,
                new ReservationData(15L, DIA, HORA, requestKey),
                AHORA);
    }

    @Test
    @DisplayName("guarda el proceso y lo vuelve a leer con todo lo que la catedra dio")
    void guardaYVuelveALeer() {
        Long userId = creaUsuario();

        ReservationProcess guardado = repository.save(
                proceso(userId, "clave-1")
                        .holdCreated("hold-6c0c2f41", "process-a92b47d0", VENCE, AHORA)
                        .orElseThrow());

        assertThat(guardado.id()).isNotNull();

        ReservationProcess leido = repository.findById(guardado.id()).orElseThrow();

        assertThat(leido.userId()).isEqualTo(userId);
        assertThat(leido.externalPatientId()).isEqualTo("user-" + userId);
        assertThat(leido.state()).isEqualTo(ReservationState.HOLD_CREATED);
        assertThat(leido.professionalId()).isEqualTo(15L);
        assertThat(leido.date()).isEqualTo(DIA);
        assertThat(leido.startTime()).isEqualTo(HORA);
        assertThat(leido.holdId()).isEqualTo("hold-6c0c2f41");
        assertThat(leido.catedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(leido.expiresAt()).isEqualTo(VENCE);
        assertThat(leido.requestKey()).isEqualTo("clave-1");
        assertThat(leido.createdAt()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("busca el proceso por la clave de idempotencia del usuario")
    void buscaPorLaClaveDeIdempotencia() {
        Long userId = creaUsuario();
        repository.save(proceso(userId, "clave-2"));

        assertThat(repository.findByRequestKey(userId, "clave-2")).isPresent();
        assertThat(repository.findByRequestKey(userId, "otra-clave")).isEmpty();
        assertThat(repository.findByRequestKey(userId + 1, "clave-2")).isEmpty();
    }

    @Test
    @DisplayName("la misma clave del mismo usuario choca con el indice y se traduce")
    void laMismaClaveChocaConElIndice() {
        Long userId = creaUsuario();
        repository.save(proceso(userId, "clave-3"));

        var error = catchThrowableOfType(
                DuplicateReservationRequestException.class,
                () -> repository.save(proceso(userId, "clave-3")));

        assertThat(error).isNotNull();
    }

    @Test
    @DisplayName("la misma clave de otro usuario si se puede guardar")
    void laMismaClaveDeOtroUsuarioSiSePuede() {
        Long primero = creaUsuario();
        Long segundo = usuarios.save(UserEntity.builder()
                .login("otro")
                .passwordHash("hash")
                .firstName("Otro")
                .lastName("Usuario")
                .email("otro@ejemplo.edu")
                .langKey("es")
                .activated(true)
                .authorities(Set.of())
                .build()).getId();

        repository.save(proceso(primero, "clave-4"));

        assertThat(repository.save(proceso(segundo, "clave-4")).id()).isNotNull();
    }

    @Test
    @DisplayName("el estado se guarda como texto, no como numero")
    void elEstadoSeGuardaComoTexto() {
        Long userId = creaUsuario();
        repository.save(proceso(userId, "clave-5"));

        ReservationProcessEntity entity = jpaRepository.findAll().getFirst();

        assertThat(entity.getStatus()).isEqualTo(ReservationState.INITIATED);
        assertThat(entity.getStatus().name()).isEqualTo("INITIATED");
    }
}
