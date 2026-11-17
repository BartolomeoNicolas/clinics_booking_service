package com.bartolomeo.clinics.booking.catedra.infrastructure.security;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraIntegrationMissingException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraRejectedException;
import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraProperties;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.CatedraLoginRequest;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.CatedraLoginResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Consigue y conserva el JWT técnico con el que se le habla a la cátedra.
 *
 * <p><strong>Esta credencial no sale nunca hacia la app.</strong> Vive solo en memoria, se pone
 * en el header interno de cada llamada saliente y no aparece en ninguna respuesta, en ningún
 * log ni en ninguna traza: la app autentica contra <em>este</em> servicio con su propio token de
 * sesión, y si pudiera ver el de la cátedra tendría el proyecto entero.
 *
 * <p><strong>Qué se guarda y qué no.</strong> Se cachea el token, que es barato de reponer y
 * caro de pedir de nuevo. No se cachea la contraseña, que se lee de configuración únicamente
 * cuando hace falta un login. El token renovado se queda acá y jamás se escribe de vuelta en el
 * almacén de secretos: un secreto que se persiste automáticamente se convierte en un secreto que
 * nadie sabe dónde está.
 *
 * <p><strong>La renovación la dispara un 401, no un reloj.</strong> Es una decisión y no
 * flojera: chequear la vigencia obligaría a parsear el JWT y a calcular con relojes de dos
 * máquinas distintas, mientras que la cátedra ya nos está diciendo que el token no sirve. El
 * adaptador reintenta una sola vez después de renovar; si vuelve a fallar, se acabó y el error
 * es de credencial, no de red. Reintentar más sería oscurecer un problema de configuración.
 *
 * <p>Es {@code synchronized} en el login a propósito: dos hilos que llegan sin token pueden
 * pedir dos, y con un token el que importa es que el que quede guardado sea válido. Un login
 * es raro y corto; no vale la pena una caché más ingeniosa.
 */
@Component
@RequiredArgsConstructor
public class CatedraTokenSource {

    private static final Logger log = LoggerFactory.getLogger(CatedraTokenSource.class);

    private final CatedraProperties properties;

    /**
     * Cliente propio para el login.
     *
     * <p>Se arma en el primer login y no en el arranque, por dos razones prácticas: una app sin
     * credenciales todavía tiene que poder levantar, y un test puede construir esta clase a mano
     * sin tener que simular el ciclo de vida de Spring.
     */
    private RestClient client;

    /** Token vigente. Puede ser nulo antes del primer uso. */
    private volatile String token;

    /**
     * El cliente del login, armado una sola vez.
     *
     * <p>Se llama solo desde {@link #login()}, que está sincronizado, así que no hace falta
     * bloqueo propio. Su tiempo de espera es el de confirmar, que es el más largo: pedir un
     * token no debería cortarse antes que la operación más larga a la que va a autorizar.
     */
    private RestClient cliente() {
        if (client == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
            requestFactory.setReadTimeout(properties.getConfirmHoldTimeoutMs());
            client = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build();
        }
        return client;
    }

    /**
     * El token para la próxima llamada.
     *
     * <p>Si hay uno en caché lo devuelve tal cual; si no, pide uno. No mira la fecha de
     * vencimiento: si está vencido la cátedra lo va a decir con un 401 y ahí se renueva.
     *
     * @return el JWT técnico, nunca nulo
     */
    public String current() {
        String existente = token;
        return existente != null ? existente : login();
    }

    /**
     * Obliga a pedir un token nuevo, descartando el que hubiera.
     *
     * <p>Se llama cuando una llamada legítima contestó 401. Un solo reintento después de esta
     * línea: si el token recién pedido también se rechaza, el problema no es la vigencia.
     *
     * @return el JWT técnico nuevo, nunca nulo
     */
    public String renew() {
        token = null;
        return login();
    }

    /** Por si una operación quiere descartar el token sin pedir otro todavía. */
    public void invalidate() {
        token = null;
    }

    private synchronized String login() {
        String existente = token;
        if (existente != null) {
            return existente;
        }
        if (!properties.isProvisioned()) {
            throw new CatedraIntegrationMissingException(
                    "Faltan las credenciales de la cuenta técnica de la cátedra: definí"
                            + " CLINICS_CATEDRA_TECHNICAL_USER y"
                            + " CLINICS_CATEDRA_TECHNICAL_PASSWORD.");
        }

        try {
            CatedraLoginResponse respuesta = cliente().post()
                    .uri("/api/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(CatedraLoginRequest.withLongSession(
                            properties.getTechnicalUser(), properties.getTechnicalPassword()))
                    .retrieve()
                    .body(CatedraLoginResponse.class);

            if (respuesta == null || respuesta.id_token() == null
                    || respuesta.id_token().isBlank()) {
                throw new CatedraRejectedException(
                        "La cátedra contestó el login sin token.", null);
            }

            log.debug("JWT técnico de la cátedra renovado");
            token = respuesta.id_token();
            return token;

        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new CatedraRejectedException(
                        "La cátedra rechazó las credenciales técnicas de este servicio (HTTP "
                                + status + ").",
                        ex);
            }
            throw new CatedraUnavailableException(
                    "La cátedra no pudo emitir el JWT técnico (HTTP " + status + ").", ex);
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw new CatedraUnavailableException(
                    "No se pudo contactar a la cátedra para pedir el JWT técnico: "
                            + ex.getMessage(),
                    ex);
        }
    }
}
