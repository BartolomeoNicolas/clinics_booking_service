package com.bartolomeo.clinics.booking;

import java.nio.file.Files;
import java.nio.file.Path;

import com.bartolomeo.clinics.booking.infrastructure.persistence.entity.UserEntity;
import com.bartolomeo.clinics.booking.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registro, inicio de sesion y perfil, contra una base real.
 *
 * <p>El contenedor de Postgres levanta el esquema con el mismo changelog de Liquibase que usa
 * el servicio, asi que el test ejercita tambien el esquema.
 *
 * <p>Las claves RSA se generan en un archivo temporal: el servicio las crea en el arranque si
 * no existen, asi que el test cubre tambien esa generacion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinics_booking")
            .withUsername("booking_app")
            .withPassword("clave-de-prueba");

    private static final Path KEY_DIR = tempKeyDir();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository jpaUserRepository;

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
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @BeforeEach
    void limpia() {
        jpaUserRepository.deleteAll();
    }

    private String registroValido() {
        return """
                {
                  "login": "nicobartok0",
                  "password": "clave-de-prueba-123",
                  "firstName": "Nicolás",
                  "lastName": "Bartolomeo",
                  "email": "nico@ejemplo.edu",
                  "langKey": "es"
                }
                """;
    }

    // ------------------------------------------------------------- registro

    @Test
    @DisplayName("registra el usuario y no devuelve la contraseña")
    void registra() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("nicobartok0"))
                .andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("la contraseña queda hasheada en la base")
    void hasheaLaContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()));

        UserEntity entity = jpaUserRepository.findByLogin("nicobartok0").orElseThrow();

        assertThat(entity.getPasswordHash()).startsWith("$2");
        assertThat(entity.getPasswordHash()).isNotEqualTo("clave-de-prueba-123");
    }

    @Test
    @DisplayName("un login duplicado se rechaza con código estable")
    void loginDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroValido().replace("nico@ejemplo.edu", "otro@ejemplo.edu")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("login-already-taken"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("un email duplicado se rechaza con código estable")
    void emailDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroValido().replace("nicobartok0", "otro")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email-already-taken"));
    }

    @Test
    @DisplayName("una entrada inválida devuelve 400 con los errores por campo")
    void validacion() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"x","password":"123","firstName":"","lastName":"","email":"no-es-un-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("inicia sesión y emite un token con los permisos del usuario")
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nicobartok0","password":"clave-de-prueba-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andExpect(jsonPath("$.scope").value("ROLE_USER"));
    }

    @Test
    @DisplayName("una contraseña incorrecta devuelve 401 con código")
    void contrasenaIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nicobartok0","password":"clave-mala"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid-credentials"));
    }

    @Test
    @DisplayName("un login inexistente da el mismo error que una contraseña mala")
    void loginInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"noexiste","password":"clave-mala"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid-credentials"));
    }

    // ------------------------------------------------------------- seguridad

    @Test
    @DisplayName("sin token no se accede a /api/me")
    void sinToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    @DisplayName("con token se accede a /api/me")
    void conToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido()));

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nicobartok0","password":"clave-de-prueba-123"}
                                """))
                .andReturn().getResponse().getContentAsString();

        String idToken = token.replaceAll(".*\"id_token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + idToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("nicobartok0"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("un token con otra firma se rechaza")
    void firmaAjena() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJuaWNvYmFydG9rMCJ9.c2lnbmF0dXJhLWZ1Y2tvby1ub3QtdmFsaWQ";

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }
}
