package com.bartolomeo.clinics.booking.catedra.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraIntegrationMissingException;
import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * El token técnico de la cátedra.
 *
 * <p><strong>El token del registro sirve y no hay que pedir otro.</strong> El registro de la
 * cuenta entrega un JWT de un año (anexo §5.1), y mandarlo es más simple que autenticarse
 * cada vez: lo que importa es que siga vigente.
 *
 * <p>La firma no se verifica acá —eso lo hace la cátedra—: lo que se comprueba es que el
 * token diga cuándo vence, porque sin esa fecha no se puede saber si conviene mandarlo.
 */
@DisplayName("el token tecnico de la catedra")
class CatedraTokenSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("usa el token de la configuracion si sigue vigente")
    void usaElTokenConfigurado() {
        CatedraProperties properties = new CatedraProperties();
        properties.setTechnicalJwt(tokenVigente(3600));
        // Sin usuario ni contraseña: si intentara autenticarse, fallaría.
        CatedraTokenSource source = new CatedraTokenSource(properties, objectMapper);

        assertThat(source.current()).isEqualTo(properties.getTechnicalJwt());
    }

    @Test
    @DisplayName("un token vencido no se usa: se pide uno nuevo")
    void unTokenVencidoNoSeUsa() {
        CatedraProperties properties = new CatedraProperties();
        properties.setTechnicalJwt(tokenVigente(-10));
        CatedraTokenSource source = new CatedraTokenSource(properties, objectMapper);

        var error = catchThrowableOfType(
                CatedraIntegrationMissingException.class, source::current);

        assertThat(error).isNotNull();
    }

    @Test
    @DisplayName("un token ilegible no se usa")
    void unTokenIlegibleNoSeUsa() {
        CatedraProperties properties = new CatedraProperties();
        properties.setTechnicalJwt("esto-no-es-un-jwt");
        CatedraTokenSource source = new CatedraTokenSource(properties, objectMapper);

        var error = catchThrowableOfType(
                CatedraIntegrationMissingException.class, source::current);

        assertThat(error).isNotNull();
    }

    /** Un JWT con la firma inventada: al código solo le interesa el {@code exp}. */
    private String tokenVigente(int segundos) {
        String cabecera = base64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = base64(
                "{\"exp\":" + (System.currentTimeMillis() / 1000 + segundos) + "}");
        return cabecera + "." + payload + ".firma-inventada";
    }

    private String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
