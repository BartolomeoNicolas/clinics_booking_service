package com.bartolomeo.clinics.booking.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.availability.application.exception.InvalidAvailabilityRangeException;
import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.availability.domain.ports.in.GetAvailabilityUseCase;
import com.bartolomeo.clinics.booking.availability.application.usecases.GetAvailabilityUseCaseImpl;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catedra.domain.ports.out.CatedraAppointmentsPort;
import com.bartolomeo.clinics.booking.catalog.application.exception.ProfessionalNotFoundException;
import com.bartolomeo.clinics.booking.catalog.domain.model.Professional;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import com.bartolomeo.clinics.booking.catalog.domain.ports.out.CatalogQueryPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Junta las tres fuentes en el orden en que se juntan, y corta antes de pagar lo que no hace
 * falta.
 *
 * <p>Tests unitarios puros con los dos puertos simulados. Lo que se prueba acá no es el
 * cálculo —eso está en {@link SlotGeneratorTest}— sino la orquestación: qué se le pregunta a
 * cada servicio, en qué orden, y qué se responde cuando una de las fuentes dice que no.
 */
@DisplayName("el caso de uso de disponibilidad")
class GetAvailabilityUseCaseImplTest {

    /** Lunes y martes consecutivos, el rango de la consulta. */
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    private static final long PROFESIONAL = 15L;

    private final CatalogQueryPort catalog = mock(CatalogQueryPort.class);
    private final CatedraAppointmentsPort catedra = mock(CatedraAppointmentsPort.class);
    private final GetAvailabilityUseCase useCase =
            new GetAvailabilityUseCaseImpl(catalog, catedra);

    /** Lunes de 09:00 a 11:00 con turnos de una hora: dos turnos, 09:00 y 10:00. */
    private static WeeklySchedule agendaDeLunes() {
        return new WeeklySchedule(1L, PROFESIONAL, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), 60, true);
    }

    private static Professional profesional(boolean enabled) {
        return new Professional(PROFESIONAL, 3L, "Ana", "Pérez", enabled);
    }

    private void responderConElProfesional(boolean enabled) {
        when(catalog.getProfessional(PROFESIONAL)).thenReturn(Optional.of(profesional(enabled)));
    }

    private void responderConLaAgenda() {
        when(catalog.getWeeklySchedules(PROFESIONAL)).thenReturn(List.of(agendaDeLunes()));
    }

    @Test
    @DisplayName("rechaza el rango invertido antes de preguntarle a nadie")
    void rangoInvertidoSeRechaza() {
        var error = catchThrowableOfType(
                InvalidAvailabilityRangeException.class,
                () -> useCase.getAvailability(PROFESIONAL, MARTES, LUNES));

        assertThat(error.getMessage().contains(LUNES.toString())).isTrue();
        verifyNoInteractions(catalog, catedra);
    }

    @Test
    @DisplayName("un profesional que el catalogo no conoce es not-found, no una lista vacia")
    void profesionalInexistenteEsNotFound() {
        when(catalog.getProfessional(PROFESIONAL)).thenReturn(Optional.empty());

        var error = catchThrowableOfType(
                ProfessionalNotFoundException.class,
                () -> useCase.getAvailability(PROFESIONAL, LUNES, MARTES));

        assertThat(error).isNotNull();
        verify(catalog, never()).getWeeklySchedules(PROFESIONAL);
        verifyNoInteractions(catedra);
    }

    @Test
    @DisplayName("un profesional deshabilitado devuelve vacio y no consulta la agenda ni la catedra")
    void profesionalDeshabilitadoDevuelveVacio() {
        responderConElProfesional(false);

        List<Slot> slots = useCase.getAvailability(PROFESIONAL, LUNES, MARTES);

        assertThat(slots).isEmpty();
        verify(catalog, never()).getWeeklySchedules(PROFESIONAL);
        verifyNoInteractions(catedra);
    }

    @Test
    @DisplayName("resta las ocupaciones confirmadas de la agenda y ofrece el resto")
    void restaLasOcupaciones() {
        responderConElProfesional(true);
        responderConLaAgenda();
        when(catedra.getOccupancies(PROFESIONAL, LUNES, MARTES)).thenReturn(List.of(
                new Occupancy(LUNES, LocalTime.of(9, 0), OccupancyStatus.CONFIRMED, null)));

        List<Slot> slots = useCase.getAvailability(PROFESIONAL, LUNES, MARTES);

        assertThat(slots)
                .extracting(Slot::startTime)
                .containsExactly(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("con la lista de ocupaciones vacia ofrece toda la agenda")
    void sinOcupacionesOfreceTodo() {
        responderConElProfesional(true);
        responderConLaAgenda();
        when(catedra.getOccupancies(PROFESIONAL, LUNES, MARTES)).thenReturn(List.of());

        List<Slot> slots = useCase.getAvailability(PROFESIONAL, LUNES, MARTES);

        assertThat(slots).extracting(Slot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("le pasa a la catedra el mismo rango que le pidio el usuario")
    void elRangoViajaTalCual() {
        responderConElProfesional(true);
        responderConLaAgenda();
        when(catedra.getOccupancies(PROFESIONAL, LUNES, MARTES)).thenReturn(List.of());

        useCase.getAvailability(PROFESIONAL, LUNES, MARTES);

        verify(catedra).getOccupancies(PROFESIONAL, LUNES, MARTES);
    }
}
