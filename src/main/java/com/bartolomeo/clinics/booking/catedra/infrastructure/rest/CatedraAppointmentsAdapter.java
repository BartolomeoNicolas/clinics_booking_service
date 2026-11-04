package com.bartolomeo.clinics.booking.catedra.infrastructure.rest;

import com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentCancellation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentConfirmation;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentHold;
import com.bartolomeo.clinics.booking.catedra.domain.model.AppointmentsQuery;
import com.bartolomeo.clinics.booking.catedra.domain.model.CatedraAppointment;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.catedra.infrastructure.config.CatedraProperties;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentCancellationRequest;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentCancellationResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldConfirmationRequest;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldConfirmationResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldRequest;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentHoldResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.AppointmentResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto.OccupanciesResponse;
import com.bartolomeo.clinics.booking.catedra.infrastructure.rest.mapper.CatedraRestMapper;
import com.bartolomeo.clinics.booking.catedra.infrastructure.security.CatedraTokenSource;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Implementa {@link CatedraAppointmentsPort} hablándole HTTP a la cátedra.
 *
 * <p>Acá está toda la tecnología de este contexto: la URL, los cinco timeouts, el header de
 * autorización, los tipos del hilo de cable, el reintento del token y la traducción de errores.
 * Del otro lado del puerto no hay nada de eso, y esa es la razón entera de tener el puerto.
 *
 * <p><strong>Un timeout por operación, y no uno global.</strong> Leer ocupaciones es lo que
 * espera el usuario con la pantalla abierta; confirmar un retén pone en marcha la creación de la
 * reserva y un timeout agresivo ahí dejaría un retén tomado del otro lado sin que nadie sepa si
 * se confirmó. Cada operación arma su propio cliente —nunca compartido con el resto de la
 * aplicación, que arrastra interceptores y tiempos ajenos— y se cachea por su tiempo de espera.
 *
 * <p><strong>La credencial la pone un interceptor, no cada método.</strong> Así ninguna llamada
 * puede olvidarse del header, y cuando el token se renueva, el siguiente intento —el mismo
 * código, sin tocar nada— ya sale con el nuevo.
 *
 * <p><strong>Los reintentos son solo para las dos lecturas.</strong> Un timeout no prueba que
 * una operación haya fallado (anexo 18.4), así que repetir una escritura a ciegas es arriesgar
 * dos efectos para ganar un reintento. Crear, confirmar y cancelar fallan a la primera; quien
 * reintenta esas es el caso de uso, mirando primero el estado por los identificadores que ya
 * guardó. Las lecturas sí se repiten, acotadas y avisadas en el log.
 *
 * <p><strong>Ningún error del otro lado se escapa como excepción del framework.</strong> Toda
 * falla pasa por {@link CatedraProblemTranslator}, que decide por {@code status} y {@code code}
 * y nunca por el texto de {@code detail}, y las excepciones que llegan a los casos de uso son
 * las de {@code catedra.application.exception}. Sin eso, los casos de uso tendrían que conocer
 * {@code RestClientResponseException} y la arquitectura se les daría vuelta.
 */
@Component
@RequiredArgsConstructor
public class CatedraAppointmentsAdapter implements CatedraAppointmentsPort {

    private static final Logger log = LoggerFactory.getLogger(CatedraAppointmentsAdapter.class);

    private final CatedraProperties properties;
    private final CatedraTokenSource tokens;
    private final CatedraProblemTranslator translator;

    /** Clientes por tiempo de espera. La clave es el timeout, porque la URL es siempre la misma. */
    private final Map<Integer, RestClient> clients = new ConcurrentHashMap<>();

    @Override
    public List<Occupancy> getOccupancies(long professionalId, LocalDate from, LocalDate to) {
        URI uri = UriComponentsBuilder.fromPath("/api/appointment-occupancies")
                .queryParam("professionalId", professionalId)
                .queryParam("from", from)
                .queryParam("to", to)
                .build()
                .encode()
                .toUri();

        OccupanciesResponse respuesta = execute(
                () -> client(properties.getOccupanciesReadTimeoutMs()).get().uri(uri),
                CatedraCall.ocupaciones(professionalId),
                true,
                OccupanciesResponse.class);

        if (respuesta == null || respuesta.occupancies() == null) {
            // Una lista ausente no es "no hay ocupaciones": es que no llegó lo que el contrato
            // garantiza, y responder con una lista vacía ofrecería todos los huecos tomados.
            throw new CatedraUnavailableException(
                    "La cátedra devolvió ocupaciones sin la lista que el contrato garantiza.");
        }
        return respuesta.occupancies().stream()
                .map(CatedraRestMapper::toOccupancy)
                .toList();
    }

    @Override
    public AppointmentHold createHold(
            long professionalId, LocalDate date, LocalTime startTime, String externalPatientId) {
        AppointmentHoldRequest cuerpo =
                new AppointmentHoldRequest(professionalId, date, startTime, externalPatientId);

        AppointmentHoldResponse respuesta = execute(
                () -> client(properties.getCreateHoldTimeoutMs()).post()
                        .uri("/api/appointment-holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo),
                CatedraCall.crearReten(professionalId, date, startTime),
                false,
                AppointmentHoldResponse.class);

        if (respuesta == null) {
            throw new CatedraUnavailableException(
                    "La cátedra creó el retén sin devolver el cuerpo que el contrato garantiza.");
        }
        return CatedraRestMapper.toHold(respuesta);
    }

    @Override
    public AppointmentConfirmation confirmHold(
            String holdId, String externalPatientId, String patientFirstName,
            String patientLastName) {
        AppointmentHoldConfirmationRequest cuerpo = new AppointmentHoldConfirmationRequest(
                externalPatientId, patientFirstName, patientLastName);

        AppointmentHoldConfirmationResponse respuesta = execute(
                () -> client(properties.getConfirmHoldTimeoutMs()).post()
                        .uri("/api/appointment-holds/{holdId}/confirm", holdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo),
                CatedraCall.confirmarReten(holdId),
                false,
                AppointmentHoldConfirmationResponse.class);

        if (respuesta == null) {
            throw new CatedraUnavailableException(
                    "La cátedra confirmó el retén sin devolver el proceso ni su vencimiento.");
        }
        return CatedraRestMapper.toConfirmation(respuesta);
    }

    @Override
    public List<CatedraAppointment> listAppointments(AppointmentsQuery query) {
        URI uri = appointmentsUri(query);

        AppointmentResponse[] respuesta = execute(
                () -> client(properties.getAppointmentsReadTimeoutMs()).get().uri(uri),
                CatedraCall.listarReservas(),
                true,
                AppointmentResponse[].class);

        if (respuesta == null) {
            throw new CatedraUnavailableException(
                    "La cátedra devolvió el listado sin el arreglo que el contrato garantiza.");
        }
        return Arrays.stream(respuesta)
                .map(CatedraRestMapper::toAppointment)
                .toList();
    }

    @Override
    public AppointmentCancellation cancelAppointment(String reservationProcessId, String reason) {
        AppointmentCancellationRequest cuerpo = new AppointmentCancellationRequest(reason);

        AppointmentCancellationResponse respuesta = execute(
                () -> client(properties.getCancelTimeoutMs()).post()
                        .uri("/api/appointments/{reservationProcessId}/cancel",
                                reservationProcessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo),
                CatedraCall.cancelar(reservationProcessId),
                false,
                AppointmentCancellationResponse.class);

        if (respuesta == null) {
            throw new CatedraUnavailableException(
                    "La cátedra no devolvió el estado final de la cancelación.");
        }
        return CatedraRestMapper.toCancellation(respuesta);
    }

    /**
     * Los filtros del listado, que son todos opcionales.
     *
     * <p>Los que vienen en nulo simplemente no se mandan: agregar un parámetro vacío es una
     * forma segura de que el otro lado filtre por vacío en lugar de no filtrar. Se codifica
     * acá —y no a mano— porque {@code externalPatientId} puede traer caracteres que rompen una
     * query armada con concatenación.
     */
    private URI appointmentsUri(AppointmentsQuery query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/appointments");
        if (query.externalPatientId() != null) {
            builder.queryParam("externalPatientId", query.externalPatientId());
        }
        if (query.status() != null) {
            builder.queryParam("status", query.status().name());
        }
        if (query.from() != null) {
            builder.queryParam("from", query.from());
        }
        if (query.to() != null) {
            builder.queryParam("to", query.to());
        }
        builder.queryParam("page", query.page());
        builder.queryParam("size", query.size());
        return builder.build().encode().toUri();
    }

    /**
     * Hace la llamada y traduce todo lo que puede salir mal a excepciones propias.
     *
     * @param request arma la petición de nuevo en cada intento, porque un {@code RestClient} no
     *                se puede reutilizar una vez usada la respuesta —y además así el header se
     *                vuelve a poner con el token vigente
     * @param contexto qué se le estaba pidiendo, para nombrar cosas reales en el mensaje
     * @param idempotente si repetir esta operación no puede producir un segundo efecto
     * @param type tipo esperado del cuerpo
     */
    private <T> T execute(Supplier<RestClient.RequestHeadersSpec<?>> request, CatedraCall contexto,
            boolean idempotente, Class<T> type) {
        return execute(request, contexto, idempotente, type, true);
    }

    /**
     * La misma llamada, con o sin permiso para renovar el token.
     *
     * <p>La recursión tiene una sola vuelta a propósito: si el token recién pedido también se
     * rechaza, el problema no es la vigencia y volver a pedir uno más solo enmascara una falla
     * de configuración. La renovación no consume los reintentos de lectura, porque ocurre
     * antes de que la petición se haya ejecutado —un 401 es una negativa definitiva, no una
     * duda sobre si pasó—.
     */
    private <T> T execute(Supplier<RestClient.RequestHeadersSpec<?>> request, CatedraCall contexto,
            boolean idempotente, Class<T> type, boolean puedeRenovar) {
        int intentos = idempotente ? properties.getReadRetries() + 1 : 1;

        for (int intento = 0; intento < intentos; intento++) {
            try {
                return request.get().retrieve().body(type);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 401 && puedeRenovar) {
                    log.debug("La cátedra rechazó el JWT técnico; se pide uno nuevo");
                    tokens.renew();
                    return execute(request, contexto, idempotente, type, false);
                }
                throw translator.translate(status, ex.getResponseBodyAsString(), contexto, ex);
            } catch (HttpMessageConversionException ex) {
                // Un cuerpo con otra forma no es una falla de red: es una violación del
                // contrato, y repetir la llamada no la va a arreglar.
                throw noDisponible(contexto, ex);
            } catch (RestClientException ex) {
                if (intento + 1 < intentos) {
                    log.warn("Reintento {} de {} para {} tras {}",
                            intento + 1, intentos - 1, contexto.operation(), ex.getMessage());
                    continue;
                }
                throw noDisponible(contexto, ex);
            }
        }

        // Inalcanzable con la lógica actual: el último intento siempre devuelve o lanza. Se
        // deja para que el compilador vea que el método no termina sin valor, y para que un
        // cambio futuro en el ciclo no empiece devolviendo nulo en silencio.
        throw new CatedraUnavailableException(
                "Se agotaron los intentos contra la cátedra al pedir " + contexto.operation() + ".");
    }

    private CatedraUnavailableException noDisponible(CatedraCall contexto, Exception causa) {
        return new CatedraUnavailableException(
                "No se pudo obtener respuesta de la cátedra al pedir " + contexto.operation()
                        + ": " + causa.getMessage(),
                causa);
    }

    /**
     * El cliente de una operación, con su propio tiempo de espera.
     *
     * <p>El token no se pone acá: lo pone un interceptor en cada petición, así que un token
     * renovado entre medio de dos llamadas no deja clientes viejos mandando uno vencido.
     */
    private RestClient client(int readTimeoutMs) {
        return clients.computeIfAbsent(readTimeoutMs, timeout -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
            requestFactory.setReadTimeout(timeout);
            return RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .requestInterceptor((httpRequest, body, execution) -> {
                        httpRequest.getHeaders().setBearerAuth(tokens.current());
                        return execution.execute(httpRequest, body);
                    })
                    .build();
        });
    }
}
