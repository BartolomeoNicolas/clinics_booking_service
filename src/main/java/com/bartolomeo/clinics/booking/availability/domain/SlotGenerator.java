package com.bartolomeo.clinics.booking.availability.domain;

import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.catedra.domain.model.Occupancy;
import com.bartolomeo.clinics.booking.catedra.domain.model.OccupancyStatus;
import com.bartolomeo.clinics.booking.catalog.domain.model.WeeklySchedule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La regla pura de la disponibilidad: de las reglas semanales y las ocupaciones, qué huecos
 * quedan libres en un rango de fechas.
 *
 * <p><strong>Es lógica de negocio sin nada del mundo exterior.</strong> No conoce Spring, ni
 * HTTP, ni la cátedra, ni el reloj: el instante "ahora" le entra como parámetro. Eso es lo que
 * permite cubrir todas sus reglas con tests de unidad corriendo en milisegundos, que es la
 * evidencia que pide BOOK-M2-02. El caso de uso es el que junta las tres fuentes y le pasa los
 * datos ya cargados.
 *
 * <p><strong>Las tres fuentes y qué aporta cada una:</strong>
 * <ul>
 *   <li>las <em>reglas semanales vigentes</em> del catálogo dicen qué días y de qué hora a
 *       qué hora se atiende, y con qué duración;</li>
 *   <li>las <em>ocupaciones</em> de la cátedra dicen qué huecos ya están tomados;</li>
 *   <li>la <em>fecha</em> elegida por el usuario dice para cuándo se pregunta.</li>
 * </ul>
 *
 * <p><strong>Tres reglas que este método no negocia:</strong>
 * <ul>
 *   <li><b>Un slot confirmado nunca se ofrece.</b> Una ocupación {@code CONFIRMED} ocupa el
 *       hueco aunque venga acompañada de un {@code HELD} en la misma posición: si la cátedra
 *       mandó ambas, prevalece la confirmada (§8), y confiar en el orden en que llegaron
 *       sería depender de algo que el contrato no promete.</li>
 *   <li><b>Un hold vencido sí se ofrece.</b> Un retén no es una reserva: cuando pasa su
 *       {@code expiresAt} el hueco quedó libre, y no ofrecerlo dejaría turnos fantasma en la
 *       pantalla. Al revés también vale: un hold sin {@code expiresAt} se trata como ocupado,
 *       porque no se puede <em>demostrar</em> que venció.</li>
 *   <li><b>Una regla apagada no genera nada.</b> El catálogo devuelve las reglas deshabilitadas
 *       incluidas y descartarlas del otro lado escondería el caso que hay que poder probar:
 *       una agenda apagada no ofrece turnos.</li>
 * </ul>
 *
 * <p><strong>Un hueco se ocupa si coincide en día y hora exactos.</strong> Es la forma en que
 * la cátedra identifica cada posición no disponible (§8), y la misma grilla con la que después
 * se crea el retén: si la posición no coincide exactamente con un turno generado, no es una
 * posición de este turno y no puede apagarlo.
 */
public final class SlotGenerator {

    private SlotGenerator() {
    }

    /**
     * Los turnos libres entre {@code from} y {@code to}, ambos inclusive.
     *
     * @param from        primer día del rango, inclusive
     * @param to          último día del rango, inclusive; se espera {@code >= from}
     * @param schedules   reglas semanales del profesional, con las apagadas incluidas
     * @param occupancies ocupaciones de la cátedra para ese mismo rango; vacía si no hay
     * @param now         instante contra el que se evalúa la vigencia de los holds
     * @return los turnos ordenados por fecha y después por hora; nunca {@code null}. Vacío si
     *         no hay reglas habilitadas para ese día
     */
    public static List<Slot> generate(
            LocalDate from,
            LocalDate to,
            List<WeeklySchedule> schedules,
            List<Occupancy> occupancies,
            Instant now) {

        Map<Posicion, Occupancy> tomados = indexarOcupaciones(occupancies);
        List<Slot> slots = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (WeeklySchedule regla : schedules) {
                if (!regla.enabled() || regla.dayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }
                agregarTurnosDeLaRegla(date, regla, tomados, now, slots);
            }
        }

        slots.sort(Comparator.comparing(Slot::date).thenComparing(Slot::startTime));
        return slots;
    }

    /**
     * Recorre la ventana de la regla de hora en hora y agrega los huecos libres.
     *
     * <p><strong>Se avanza en segundos desde medianoche y no sumando horas al
     * {@link LocalTime}.</strong> Una hora más la duración da vuelta sobre las 24 y un bucle que
     * compara contra la hora de fin puede quedarse girando para siempre —23:00 más dos horas
     * es 01:00, que sigue siendo "antes" que las 23:30—. Contando segundos el número crece
     * siempre y el bucle termina sí o sí, también con una duración absurda.
     */
    private static void agregarTurnosDeLaRegla(
            LocalDate date,
            WeeklySchedule regla,
            Map<Posicion, Occupancy> tomados,
            Instant now,
            List<Slot> slots) {

        long inicio = regla.startTime().toSecondOfDay();
        long fin = regla.endTime().toSecondOfDay();
        long duracion = regla.slotDurationMinutes() * 60L;

        // Una duración no positiva no es un turno, y una ventana invertida no tiene huecos:
        // los dos casos se descartan en lugar de girar o de inventar una hora.
        if (duracion <= 0 || inicio >= fin) {
            return;
        }

        for (long segundo = inicio; segundo + duracion <= fin; segundo += duracion) {
            LocalTime startTime = LocalTime.ofSecondOfDay(segundo);
            LocalTime endTime = LocalTime.ofSecondOfDay(segundo + duracion);

            Occupancy ocupada = tomados.get(new Posicion(date, startTime));
            if (ocupada != null && sigueOcupada(ocupada, now)) {
                continue;
            }
            slots.add(new Slot(date, startTime, endTime));
        }
    }

    /**
     * Si la ocupación sigue ocupando el hueco en el instante dado.
     *
     * <p>Una confirmación ocupa siempre. Un retén ocupa mientras su vencimiento no haya
     * pasado, y también cuando no trae vencimiento: sin esa fecha no hay forma de probar que
     * quedó libre, y equivocarse para el lado de ofrecer algo tomado es el error caro.
     */
    private static boolean sigueOcupada(Occupancy ocupada, Instant now) {
        if (ocupada.status() == OccupancyStatus.CONFIRMED) {
            return true;
        }
        return ocupada.expiresAt() == null || ocupada.expiresAt().isAfter(now);
    }

    /**
     * Las ocupaciones indexadas por la posición que tapan, con {@code CONFIRMED} por encima.
     *
     * <p>El índice se arma una sola vez y no se busca linealmente por cada turno: un día
     * completo puede tener decenas de huecos y una lista recorrida por cada uno se pone
     * cuadrática sin ningún motivo.
     */
    private static Map<Posicion, Occupancy> indexarOcupaciones(List<Occupancy> occupancies) {
        Map<Posicion, Occupancy> mapa = new HashMap<>();
        for (Occupancy ocupada : occupancies) {
            Posicion posicion = new Posicion(ocupada.date(), ocupada.startTime());
            Occupancy anterior = mapa.get(posicion);
            boolean llegoConfirmada = ocupada.status() == OccupancyStatus.CONFIRMED;
            boolean loQueHayNoEstaFijo =
                    anterior == null || anterior.status() == OccupancyStatus.HELD;
            if (llegoConfirmada || loQueHayNoEstaFijo) {
                mapa.put(posicion, ocupada);
            }
        }
        return mapa;
    }

    /**
     * Un lugar exacto de la agenda: día y hora de inicio.
     *
     * <p>Sin la hora de fin porque no es parte de la identidad: dos turnos distintos no pueden
     * empezar a la misma vez, pero una ocupación no sabe cuánto dura el turno que tapa —solo
     * dice dónde empieza—, y darle un fin sería inventar un dato que no llega.
     */
    private record Posicion(LocalDate date, LocalTime startTime) {
    }
}
