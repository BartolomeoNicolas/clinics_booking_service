package com.bartolomeo.clinics.booking.availability.domain.ports.in;

import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import java.time.LocalDate;
import java.util.List;

/**
 * Los turnos ofertables de un profesional para una fecha elegida.
 */
public interface GetAvailabilityUseCase {

    /**
     * Combina la agenda semanal vigente del catálogo con las ocupaciones informadas por la
     * cátedra y devuelve lo que quedó libre.
     *
     * <p>La fecha la elige el usuario y la trae la pantalla: este caso no mira "hoy" ni decide
     * por él. El rango va completo porque de otro lado se arriesga a responder "no hay nada"
     * cuando en realidad la pregunta estaba mal hecha.
     *
     * @param professionalId profesional cuya agenda se consulta, tal como salió del catálogo
     * @param from           primer día del rango, inclusive
     * @param to             último día del rango, inclusive; nunca anterior a {@code from}
     * @return los turnos libres ordenados por fecha y hora; vacío si no queda ninguno
     * @throws com.bartolomeo.clinics.booking.availability.application.exception.InvalidAvailabilityRangeException
     *         si {@code from} es posterior a {@code to}
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException
     *         si el catálogo no conoce ese profesional
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.CatalogUnavailableException
     *         si el catálogo no contestó
     * @throws com.bartolomeo.clinics.booking.catedra.application.exception.CatedraUnavailableException
     *         si la cátedra no contestó: sin las ocupaciones no se puede afirmar que un hueco
     *         está libre
     */
    List<Slot> getAvailability(long professionalId, LocalDate from, LocalDate to);
}
