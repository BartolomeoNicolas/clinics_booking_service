package com.bartolomeo.clinics.booking.availability.application.usecases;

import com.bartolomeo.clinics.booking.availability.application.exception.InvalidAvailabilityRangeException;
import com.bartolomeo.clinics.booking.availability.domain.SlotGenerator;
import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.availability.domain.ports.in.GetAvailabilityUseCase;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import com.bartolomeo.clinics.booking.catalog.domain.ports.out.CatalogQueryPort;
import com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Junta las tres fuentes de la disponibilidad y le pasa los datos ya cargados al generador.
 *
 * <p>Las tres fuentes son dos llamadas al catálogo y una a la cátedra, y el orden en que se
 * hacen no es arbitrario: primero se pide el profesional —es la llamada que sabe decir 404 y
 * la que trae {@code enabled}—, y recién después se paga la agenda y las ocupaciones. Una
 * agenda apagada corta antes de molestar a la cátedra, que es la más cara de las tres.
 *
 * <p><strong>Un profesional deshabilitado devuelve la lista vacía y no un error.</strong> No
 * es lo mismo que no haber preguntado: se miró la agenda, está apagada y hoy no se ofrecen
 * turnos, que es exactamente lo que la pantalla tiene que mostrar. Rechazar con un error
 * obligaría a la app a distinguir dos casos que para el usuario son el mismo.
 *
 * <p><strong>El reloj entra acá y no dentro del generador.</strong> Es la única decisión de
 * "cuándo es ahora" que vive en la capa de aplicación; el cálculo en sí recibe el instante
 * como un dato y por eso se puede probar con la hora que haga falta.
 */
@Component
@RequiredArgsConstructor
public class GetAvailabilityUseCaseImpl implements GetAvailabilityUseCase {

    private final CatalogQueryPort catalog;
    private final CatedraAppointmentsPort catedra;

    @Override
    public List<Slot> getAvailability(long professionalId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new InvalidAvailabilityRangeException(from, to);
        }

        Professional profesional = catalog.getProfessional(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));

        if (!profesional.enabled()) {
            return List.of();
        }

        List<WeeklySchedule> schedules = catalog.getWeeklySchedules(professionalId);
        List<Occupancy> occupancies = catedra.getOccupancies(professionalId, from, to);

        return SlotGenerator.generate(from, to, schedules, occupancies, Instant.now());
    }
}
