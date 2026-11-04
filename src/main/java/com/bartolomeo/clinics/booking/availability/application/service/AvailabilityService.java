package com.bartolomeo.clinics.booking.availability.application.service;

import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.availability.domain.ports.in.GetAvailabilityUseCase;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fachada del contexto de disponibilidad.
 *
 * <p>Es lo que inyecta el controller: el controller nunca toca un port {@code in} directamente
 * —aunque en este caso la fachada solo delegue—, porque el día que haya que resolver algo de
 * dos casos de uso juntos, o excepciones, o cachear, ese lugar es este y no el controller.
 *
 * <p>No toca los puertos de salida: ni el catálogo ni la cátedra. Habla únicamente con el caso
 * de uso, que es el que sabe qué fuentes juntar y en qué orden.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    /**
     * Los turnos libres de un profesional entre dos fechas, ambas inclusive.
     *
     * @throws com.bartolomeo.clinics.booking.availability.application.exception.InvalidAvailabilityRangeException
     *         si el rango viene invertido
     * @throws com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException
     *         si el catálogo no conoce al profesional
     */
    public List<Slot> getAvailability(long professionalId, LocalDate from, LocalDate to) {
        return getAvailabilityUseCase.getAvailability(professionalId, from, to);
    }
}
