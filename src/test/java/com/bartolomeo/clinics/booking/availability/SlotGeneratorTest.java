package com.bartolomeo.clinics.booking.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.bartolomeo.clinics.booking.availability.domain.SlotGenerator;
import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las tres reglas de la disponibilidad, cubiertas sin Spring, sin red y sin reloj.
 *
 * <p>Éste es el test que pide BOOK-M2-02 como evidencia, y no es una formalidad: la regla de
 * qué se ofrece y qué no es el contrato con el usuario —"te ofrezco un turno y después no lo
 * puedo dar"—, y probarla contra una base de datos y dos servicios de verdad la volvería
 * lenta, parcial y dependiente de que los otros dos estén levantados. Acá el "ahora" entra
 * como parámetro y cada caso se puede afirmar con certeza.
 *
 * <p><strong>Los tres criterios del issue tienen su test con nombre propio:</strong> un
 * confirmado nunca se ofrece, un hold vencido sí se ofrece, y una agenda deshabilitada no
 * genera nada. Alrededor están las reglas que se deciden al leer el contrato y que conviene no
 * perder de vista si alguna vez se toca este cálculo.
 */
@DisplayName("el generador de slots")
class SlotGeneratorTest {

    /** Miércoles 2026-11-04, el "ahora" contra el que se juzga la vigencia de los reténes. */
    private static final Instant AHORA = Instant.parse("2026-11-04T12:00:00Z");

    /** Lunes y martes consecutivos, para probar rangos de más de un día. */
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime TRECE = LocalTime.of(13, 0);

    // ---------------------------------------------------------------- helpers

    private static WeeklySchedule regla(
            DayOfWeek dia, LocalTime inicio, LocalTime fin, int duracion, boolean habilitada) {
        return new WeeklySchedule(1L, 15L, dia, inicio, fin, duracion, habilitada);
    }

    private static WeeklySchedule lunesHabilitada(int duracion) {
        return regla(DayOfWeek.MONDAY, NUEVE, TRECE, duracion, true);
    }

    private static Occupancy ocupada(
            LocalDate date, LocalTime startTime, OccupancyStatus status, Instant expiresAt) {
        return new Occupancy(date, startTime, status, expiresAt);
    }

    private static List<Slot> calcular(List<WeeklySchedule> schedules, List<Occupancy> ocupadas) {
        return SlotGenerator.generate(LUNES, MARTES, schedules, ocupadas, AHORA);
    }

    private static List<Slot> deUnDia(List<WeeklySchedule> schedules, List<Occupancy> ocupadas) {
        return SlotGenerator.generate(LUNES, LUNES, schedules, ocupadas, AHORA);
    }

    private static List<LocalTime> inicios(List<Slot> slots) {
        return slots.stream().map(Slot::startTime).toList();
    }

    // -------------------------------------------------- la regla de la duracion

    @Test
    @DisplayName("corta la ventana en turnos de la duracion y el ultimo termina en el fin")
    void cortaLaVentanaEnTurnos() {
        List<Slot> slots = deUnDia(List.of(lunesHabilitada(30)), List.of());

        assertThat(inicios(slots))
                .containsExactly(
                        LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                        LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30),
                        LocalTime.of(12, 0), LocalTime.of(12, 30));
        assertThat(slots.getFirst().endTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots.getLast().endTime()).isEqualTo(TRECE);
    }

    @Test
    @DisplayName("una duracion que no divide a la ventana deja el resto fuera, no lo estira")
    void unaDuracionQueNoDivideNoEstiraElUltimo() {
        List<Slot> slots = deUnDia(List.of(lunesHabilitada(45)), List.of());

        // 09:00 09:45 10:30 11:15 12:00 y el siguiente cerraria a 13:30: no entra.
        assertThat(inicios(slots))
                .containsExactly(
                        LocalTime.of(9, 0), LocalTime.of(9, 45), LocalTime.of(10, 30),
                        LocalTime.of(11, 15), LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("una duracion no positiva no genera turnos y no se queda girando")
    void duracionNoPositivaNoGenera() {
        assertThat(deUnDia(List.of(lunesHabilitada(0)), List.of())).isEmpty();
        assertThat(deUnDia(
                        List.of(regla(DayOfWeek.MONDAY, NUEVE, TRECE, -30, true)), List.of()))
                .isEmpty();
    }

    @Test
    @DisplayName("una ventana invertida no genera turnos")
    void ventanaInvertidaNoGenera() {
        WeeklySchedule invertida =
                regla(DayOfWeek.MONDAY, TRECE, NUEVE, 30, true);

        assertThat(deUnDia(List.of(invertida), List.of())).isEmpty();
    }

    @Test
    @DisplayName("un turno que cruzaria medianoche no se da vuelta ni inventa horas")
    void noCruzaMedianoche() {
        // Con 23:00 + 2 horas la hora da vuelta sobre las 24 y vuelve a quedar "antes" que el
        // fin: un bucle que suma horas a LocalTime se quedaria girando para siempre.
        WeeklySchedule ultimaHora =
                regla(DayOfWeek.MONDAY, LocalTime.of(23, 0), LocalTime.of(23, 30), 120, true);

        assertThat(deUnDia(List.of(ultimaHora), List.of())).isEmpty();
    }

    // ------------------------------------------------------- los dias y el rango

    @Test
    @DisplayName("solo genera en los dias que coinciden con la regla")
    void soloLosDiasDeLaRegla() {
        List<Slot> slots = calcular(List.of(lunesHabilitada(30)), List.of());

        assertThat(slots).isNotEmpty();
        assertThat(slots).allSatisfy(slot -> assertThat(slot.date()).isEqualTo(LUNES));
    }

    @Test
    @DisplayName("el rango es inclusivo en los dos extremos")
    void elRangoEsInclusivo() {
        WeeklySchedule lunes = regla(DayOfWeek.MONDAY, NUEVE, LocalTime.of(10, 0), 60, true);
        WeeklySchedule martes = regla(DayOfWeek.TUESDAY, NUEVE, LocalTime.of(10, 0), 60, true);

        List<Slot> slots = calcular(List.of(martes, lunes), List.of());

        // Un turno en cada extremo: si alguno de los dos quedara afuera, desapareceria.
        assertThat(slots).extracting(Slot::date).containsExactly(LUNES, MARTES);
    }

    @Test
    @DisplayName("los turnos salen ordenados por fecha y despues por hora")
    void ordenadosPorFechaYHora() {
        WeeklySchedule mañana = regla(DayOfWeek.MONDAY, NUEVE, LocalTime.of(10, 0), 60, true);
        WeeklySchedule tarde = regla(DayOfWeek.MONDAY, LocalTime.of(16, 0),
                LocalTime.of(17, 0), 60, true);
        WeeklySchedule martes = regla(DayOfWeek.TUESDAY, NUEVE, LocalTime.of(10, 0), 60, true);

        // Llegan en el orden en que estan las reglas, no en el orden en que se responden.
        List<Slot> slots = calcular(List.of(tarde, mañana, martes), List.of());

        assertThat(inicios(slots)).containsExactly(NUEVE, LocalTime.of(16, 0), NUEVE);
        assertThat(slots).extracting(Slot::date).containsExactly(LUNES, LUNES, MARTES);
    }

    @Test
    @DisplayName("sin reglas habilitadas para el dia no hay turnos")
    void sinReglasNoHayTurnos() {
        WeeklySchedule apagada =
                regla(DayOfWeek.MONDAY, NUEVE, TRECE, 30, false);

        assertThat(deUnDia(List.of(apagada), List.of())).isEmpty();
        assertThat(deUnDia(List.of(), List.of())).isEmpty();
    }

    // --------------------------------------- criterio: un confirmado nunca se ofrece

    @Test
    @DisplayName("un slot confirmado por otra reserva nunca se ofrece")
    void unConfirmadoNuncaSeOfrece() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.CONFIRMED, null)));

        assertThat(inicios(slots)).doesNotContain(LocalTime.of(10, 0));
        assertThat(slots).hasSize(7);
    }

    @Test
    @DisplayName("la confirmada prevalece aunque en la misma posicion haya un reten vencido")
    void laConfirmadaPrevaleceSobreElReten() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(
                        ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD,
                                AHORA.minusSeconds(60)),
                        ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.CONFIRMED, null)));

        assertThat(inicios(slots)).doesNotContain(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("la confirmada sigue prevaleciendo aunque llegue despues que el reten")
    void laConfirmadaPrevaleceEnCualquierOrden() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(
                        ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.CONFIRMED, null),
                        ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD,
                                AHORA.minusSeconds(60))));

        assertThat(inicios(slots)).doesNotContain(LocalTime.of(10, 0));
    }

    // ------------------------------------------ criterio: un hold vencido si se ofrece

    @Test
    @DisplayName("un hold vencido si se ofrece")
    void unHoldVencidoSiSeOfrece() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD,
                        AHORA.minusSeconds(1))));

        assertThat(inicios(slots)).contains(LocalTime.of(10, 0));
        assertThat(slots).hasSize(8);
    }

    @Test
    @DisplayName("un hold que vence justo ahora ya no bloquea")
    void unHoldQueVenceAhoraYaNoBloquea() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD, AHORA)));

        assertThat(inicios(slots)).contains(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("un hold vigente si bloquea")
    void unHoldVigenteSiBloquea() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD,
                        AHORA.plusSeconds(300))));

        assertThat(inicios(slots)).doesNotContain(LocalTime.of(10, 0));
        assertThat(slots).hasSize(7);
    }

    @Test
    @DisplayName("un reten sin vencimiento se trata como ocupado: no se puede probar que vencio")
    void unRetenSinVencimientoOcupa() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.HELD, null)));

        assertThat(inicios(slots)).doesNotContain(LocalTime.of(10, 0));
    }

    // ------------------------------------- criterio: la agenda deshabilitada no genera nada

    @Test
    @DisplayName("la agenda deshabilitada del profesional no genera slots")
    void agendaDeshabilitadaNoGenera() {
        WeeklySchedule apagada = regla(DayOfWeek.MONDAY, NUEVE, TRECE, 30, false);

        assertThat(deUnDia(List.of(apagada),
                        List.of(ocupada(LUNES, LocalTime.of(9, 0),
                                OccupancyStatus.CONFIRMED, null))))
                .isEmpty();
    }

    @Test
    @DisplayName("una regla apagada no genera, aunque las demas del mismo dia si")
    void unaReglaApagadaNoArrastraALasOtras() {
        WeeklySchedule apagada = regla(DayOfWeek.MONDAY, LocalTime.of(16, 0),
                LocalTime.of(17, 0), 60, false);

        List<Slot> slots = deUnDia(List.of(lunesHabilitada(60), apagada), List.of());

        assertThat(inicios(slots))
                .containsExactly(
                        NUEVE, LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0))
                .doesNotContain(LocalTime.of(16, 0));
    }

    // --------------------------------------------------- la ocupacion y la coincidencia

    @Test
    @DisplayName("la ocupacion coincide en dia y hora exactos, y en ninguna otra forma")
    void laCoincidenciaEsExacta() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(
                        // Otro dia: ni se acerca.
                        ocupada(MARTES, LocalTime.of(10, 0), OccupancyStatus.CONFIRMED, null),
                        // Misma hora pero distinto dia de la regla.
                        ocupada(MARTES, LocalTime.of(9, 0), OccupancyStatus.CONFIRMED, null),
                        // Medio turno desfasado: no es una posicion de esta grilla.
                        ocupada(LUNES, LocalTime.of(9, 15), OccupancyStatus.CONFIRMED, null)));

        assertThat(inicios(slots)).hasSize(8).contains(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("cada posicion tomada apaga solo su turno")
    void cadaOcupadaApagaSoloElSuyo() {
        List<Slot> slots = deUnDia(
                List.of(lunesHabilitada(30)),
                List.of(
                        ocupada(LUNES, LocalTime.of(9, 0), OccupancyStatus.CONFIRMED, null),
                        ocupada(LUNES, LocalTime.of(12, 30), OccupancyStatus.CONFIRMED, null)));

        assertThat(inicios(slots))
                .doesNotContain(LocalTime.of(9, 0), LocalTime.of(12, 30))
                .hasSize(6);
    }

    @Test
    @DisplayName("con todas las posiciones ocupadas devuelve la lista vacia, no nulo")
    void todoOcupadoDevuelveVacio() {
        List<WeeklySchedule> una = List.of(lunesHabilitada(60));
        List<Occupancy> todas = List.of(
                ocupada(LUNES, NUEVE, OccupancyStatus.CONFIRMED, null),
                ocupada(LUNES, LocalTime.of(10, 0), OccupancyStatus.CONFIRMED, null),
                ocupada(LUNES, LocalTime.of(11, 0), OccupancyStatus.CONFIRMED, null),
                ocupada(LUNES, LocalTime.of(12, 0), OccupancyStatus.CONFIRMED, null));

        assertThat(deUnDia(una, todas)).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("con la lista de ocupaciones vacia ofrece toda la agenda")
    void sinOcupacionesOfreceTodo() {
        assertThat(deUnDia(List.of(lunesHabilitada(60)), List.of())).hasSize(4);
    }
}
