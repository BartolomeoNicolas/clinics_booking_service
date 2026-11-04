package com.bartolomeo.clinics.booking.catalog.domain.ports.out;

import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.ProfessionalCategory;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import java.util.List;
import java.util.Optional;

/**
 * Lo que este servicio lee del servicio de catálogo.
 *
 * <p><strong>Es el único camino hacia el catálogo, y no hay otro.</strong> El enunciado
 * prohíbe que turnos replique el catálogo (PLAN H4) y exige obtener de ahí la información
 * vigente antes de operar (§4.2). Este puerto es esa puerta: los casos de uso de
 * {@code availability} y de {@code reservation} lo inyectan, y lo que está del otro lado es
 * una llamada HTTP. Ninguno de ellos sabe con qué servicio habla ni con qué token.
 *
 * <p><strong>Nada de esto se persiste.</strong> Los tres métodos devuelven la respuesta de la
 * llamada y no dejan rastro: si mañana el catálogo cambia un horario, la próxima consulta
 * trae el nuevo. Guardar una copia local sería exactamente la réplica que el enunciado
 * prohíbe.
 *
 * <p>Las tres operaciones son <strong>idempotentes y sin efectos</strong>: repetirlas deja el
 * mismo estado que no repetirlas, porque no hay estado propio que alterar.
 */
public interface CatalogQueryPort {

    /**
     * La ficha de un profesional.
     *
     * <p>La respuesta del catálogo trae además su agenda semanal, y este método la deja
     * pasar: pedirla tiene su propio camino en {@link #getWeeklySchedules}, y traerla acá
     * obligaría a modelar juntos dos cosas que los casos de uso usan por separado.
     *
     * @param id identificador del profesional
     * @return el profesional si existe; vacío si el catálogo responde 404. Nunca {@code null}
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogRejectedException
     *         si el catálogo rechaza la credencial
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException
     *         si el catálogo no está disponible
     */
    Optional<Professional> getProfessional(long id);

    /**
     * La agenda semanal vigente de un profesional, con sus reglas apagadas incluidas.
     *
     * <p><strong>Aquí un 404 es un error y no una lista vacía.</strong> Es la diferencia con
     * {@link #getProfessional}: pedir la agenda de alguien que no existe es una pregunta mal
     * hecha —el id no vino del catálogo—, y responder "no tiene horarios" diría lo contrario
     * de lo que pasó. Quien llama tiene que enterarse de que el profesional no está.
     *
     * @param professionalId identificador del profesional
     * @return sus reglas, en el orden en que el catálogo las devuelve. Nunca {@code null};
     *         vacía si el profesional existe y todavía no tiene ninguna
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException
     *         si el profesional no existe en el catálogo
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogRejectedException
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException
     */
    List<WeeklySchedule> getWeeklySchedules(long professionalId);

    /**
     * Todas las categorías, para mostrar o validar contra el catálogo.
     *
     * @return las categorías, incluidas las deshabilitadas. Nunca {@code null}
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogRejectedException
     *         si el catálogo rechaza la credencial
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException
     *         si el catálogo no está disponible, incluido si no expone este recurso
     */
    List<ProfessionalCategory> getCategories();
}
