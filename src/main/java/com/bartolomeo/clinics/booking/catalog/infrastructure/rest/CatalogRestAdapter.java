package com.bartolomeo.clinics.booking.catalog.infrastructure.rest;

import com.bartolomeo.clinics.booking.catalog.application.exception.CatalogRejectedException;
import com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException;
import com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException;
import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.ProfessionalCategory;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import com.bartolomeo.clinics.booking.catalog.domain.ports.out.CatalogQueryPort;
import com.bartolomeo.clinics.booking.catalog.infrastructure.config.CatalogProperties;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogProfessionalCategoryResponse;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogProfessionalResponse;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.dto.CatalogWeeklyScheduleResponse;
import com.bartolomeo.clinics.booking.catalog.infrastructure.rest.mapper.CatalogRestMapper;
import com.bartolomeo.clinics.booking.catalog.infrastructure.security.ServiceTokenIssuer;
import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementa {@link CatalogQueryPort} hablándole HTTP al servicio de catálogo.
 *
 * <p>Acá está toda la tecnología de este contexto: la URL, los tiempos, el header de
 * autorización, los tipos del hilo de cable y el mapeo de errores. Del otro lado del puerto no
 * hay nada de esto, y esa es la razón entera de tener el puerto.
 *
 * <p><strong>El cliente es propio y no el autoconfigurado de Spring.</strong> Como hace el
 * cliente de la cátedra en el otro servicio, este no toma el {@code RestClient.Builder} del
 * contenedor: un cliente compartido con el resto de la aplicación arrastra sus interceptores,
 * sus cookies y sus timeouts ajenos, y cuando un catálogo se cuelga el síntoma aparece en un
 * lugar que no es este. Tiene los suyos, que es lo que permite decir "dos segundos de conexión
 * y cinco de espera" en la configuración.
 *
 * <p><strong>Ningún error del otro lado se escapa como excepción del framework.</strong> Toda
 * falla se traduce antes de cruzar el puerto:
 *
 * <ul>
 *   <li>404 — no existe, y cada método decide qué significa para su caso.</li>
 *   <li>401 y 403 — {@link CatalogRejectedException}, que es una falla nuestra de credencial y
 *       no del usuario.</li>
 *   <li>5xx, timeout, conexión rechazada y JSON fuera de contrato —
 *       {@link CatalogUnavailableException}, con la causa original adjunta.</li>
 * </ul>
 *
 * <p>Sin esa traducción, los casos de uso tendrían que conocer
 * {@code RestClientResponseException} y la arquitectura se les daría vuelta.
 */
@Component
@RequiredArgsConstructor
public class CatalogRestAdapter implements CatalogQueryPort {

    private final CatalogProperties properties;
    private final ServiceTokenIssuer tokenIssuer;

    private RestClient client;

    @PostConstruct
    void buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<Professional> getProfessional(long id) {
        CatalogProfessionalResponse response =
                exchange("/api/catalog/professionals/{id}", new Object[] {id},
                        CatalogProfessionalResponse.class);
        if (response == null) {
            return Optional.empty();
        }
        return Optional.of(CatalogRestMapper.toProfessional(response));
    }

    @Override
    public List<WeeklySchedule> getWeeklySchedules(long professionalId) {
        CatalogWeeklyScheduleResponse[] response =
                exchange("/api/catalog/professionals/{id}/weekly-schedules",
                        new Object[] {professionalId}, CatalogWeeklyScheduleResponse[].class);
        if (response == null) {
            throw new ProfessionalNotFoundException(professionalId);
        }
        try {
            return Arrays.stream(response)
                    .map(CatalogRestMapper::toWeeklySchedule)
                    .toList();
        } catch (IllegalArgumentException ex) {
            // Un dia que no existe en la enumeracion es una violacion del contrato, no un
            // usuario equivocado: no se devuelve una agenda vacia como si no hubiera nada.
            throw new CatalogUnavailableException(
                    "El catálogo devolvió una regla de agenda que no coincide con el contrato"
                            + " esperado: " + ex.getMessage(),
                    ex);
        }
    }

    @Override
    public List<ProfessionalCategory> getCategories() {
        CatalogProfessionalCategoryResponse[] response =
                exchange("/api/catalog/categories", new Object[0],
                        CatalogProfessionalCategoryResponse[].class);
        if (response == null) {
            // 404 aca no significa "no hay categorias": significa que el recurso que este
            // servicio espera que exista no existe, que es una falla de integracion.
            throw new CatalogUnavailableException(
                    "El catálogo no expone GET /api/catalog/categories");
        }
        return Arrays.stream(response)
                .map(CatalogRestMapper::toProfessionalCategory)
                .toList();
    }

    /**
     * Hace la llamada y traduce todo lo que puede salir mal a excepciones propias.
     *
     * <p>Devuelve {@code null} cuando el catálogo responde 404, porque "no existe" no significa
     * lo mismo en los tres métodos: en la ficha es una respuesta normal, en la agenda es un
     * error, y en la lista de categorías es un recurso que no estaba. Esa interpretación es de
     * quien pregunta y no del que lleva la pregunta, así que el traductor la deja afuera.
     *
     * <p>Si la respuesta viene pero no es la forma que este servicio espera —un campo de otro
     * tipo, un arreglo donde había un objeto—, también acá y no en el mapper: Jackson levanta
     * su excepción al leer el cuerpo y se convierte en
     * {@link CatalogUnavailableException} con esa causa pegada.
     */
    private <T> T exchange(String path, Object[] variables, Class<T> type) {
        try {
            return client.get()
                    .uri(path, variables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue())
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                // "No existe" lo interpreta quien pregunta, no quien lleva la pregunta.
                return null;
            }
            throw translate(status, ex);
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw new CatalogUnavailableException(
                    "No se pudo leer la respuesta del catálogo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Del código de estado que mandó el catálogo al error propio que se le devuelve a afuera.
     *
     * <p>Es un caso exhaustivo a propósito: si el catálogo empieza a mandar un estado que nadie
     * previó, cae en el último renglón y queda registrado con su número, en vez de que una
     * función que crece por {@code if}s termine devolviendo algo sin querer.
     */
    private ApplicationException translate(int status, RestClientResponseException cause) {
        if (status == 401 || status == 403) {
            return new CatalogRejectedException(
                    "El catálogo rechazó la credencial de este servicio (HTTP " + status + ").",
                    cause);
        }
        if (status >= 500) {
            return new CatalogUnavailableException(
                    "El catálogo falló con HTTP " + status + ".", cause);
        }
        return new CatalogUnavailableException(
                "Respuesta inesperada del catálogo: HTTP " + status
                        + ", que no forma parte del contrato.",
                cause);
    }
}
