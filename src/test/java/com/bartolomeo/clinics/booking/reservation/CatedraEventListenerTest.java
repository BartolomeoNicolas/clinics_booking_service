package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bartolomeo.clinics.booking.reservation.application.exception.InvalidCatedraEventException;
import com.bartolomeo.clinics.booking.reservation.application.exception.UnknownReservationProcessException;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEvent;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationEventEnvelope;
import com.bartolomeo.clinics.booking.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.bartolomeo.clinics.booking.reservation.domain.ports.out.FailedEventRepository;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.CatedraEventListener;
import com.bartolomeo.clinics.booking.reservation.infrastructure.messaging.mapper.CatedraEventMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

/**
 * El listener de los eventos de la cátedra.
 *
 * <p>Lo que se prueba acá es la disciplina del offset, que es lo que hace que un mensaje no
 * se pierda y que uno repetido no haga daño: se confirma después de persistir, lo que no se
 * puede arreglar se registra y se confirma, y lo que sí se puede arreglar no se confirma
 * para que el mensaje vuelva a llegar.
 */
@DisplayName("el listener de eventos de la catedra")
class CatedraEventListenerTest {

    private static final String TOPIC = "alumnos.turnos.acciones.nicobartok0-prog2";

    private final ProcessReservationEventUseCase processEvent =
            mock(ProcessReservationEventUseCase.class);
    private final FailedEventRepository failedEvents = mock(FailedEventRepository.class);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    private CatedraEventListener listener;

    @BeforeEach
    void preparar() {
        listener = new CatedraEventListener(
                new CatedraEventMapper(new ObjectMapper()),
                processEvent,
                failedEvents);
    }

    private ConsumerRecord<String, String> mensaje(String cuerpo) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "process-a92b47d0", cuerpo);
    }

    private static String eventoValido() {
        return """
                {
                  "eventId": "evento-1",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-11-04T12:00:00Z",
                  "schemaVersion": 1
                }
                """;
    }

    @Test
    @DisplayName("aplica el evento y confirma el offset despues de persistir")
    void aplicaYConfirmaDespues() {
        listener.onCatedraEvent(mensaje(eventoValido()), acknowledgment);

        InOrder orden = inOrder(processEvent, acknowledgment);
        orden.verify(processEvent).process(any());
        orden.verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("los campos desconocidos del mensaje se ignoran")
    void losCamposDesconocidosSeIgnoran() {
        String cuerpo = """
                {
                  "eventId": "evento-2",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "occurredAt": "2026-11-04T12:00:00Z",
                  "schemaVersion": 1,
                  "campoDelFuturo": "lo que sea"
                }
                """;

        listener.onCatedraEvent(mensaje(cuerpo), acknowledgment);

        verify(processEvent).process(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("un mensaje ilegible se registra y se confirma")
    void unMensajeIlegibleSeRegistra() {
        listener.onCatedraEvent(mensaje("{ esto no es json"), acknowledgment);

        verify(processEvent, never()).process(any());
        verify(failedEvents).save(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("un mensaje sin eventId se registra y se confirma")
    void unMensajeSinIdentificadorSeRegistra() {
        String cuerpo = """
                {
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "schemaVersion": 1
                }
                """;

        listener.onCatedraEvent(mensaje(cuerpo), acknowledgment);

        verify(processEvent, never()).process(any());
        verify(failedEvents).save(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("una version de contrato desconocida se registra y se confirma")
    void unaVersionDesconocidaSeRegistra() {
        String cuerpo = """
                {
                  "eventId": "evento-3",
                  "eventType": "AppointmentConfirmed",
                  "reservationProcessId": "process-a92b47d0",
                  "schemaVersion": 2
                }
                """;

        listener.onCatedraEvent(mensaje(cuerpo), acknowledgment);

        verify(processEvent, never()).process(any());
        verify(failedEvents).save(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("un tipo de evento desconocido se registra y se confirma")
    void unTipoDesconocidoSeRegistra() {
        String cuerpo = """
                {
                  "eventId": "evento-4",
                  "eventType": "EventoDelFuturo",
                  "reservationProcessId": "process-a92b47d0",
                  "schemaVersion": 1
                }
                """;

        listener.onCatedraEvent(mensaje(cuerpo), acknowledgment);

        verify(processEvent, never()).process(any());
        verify(failedEvents).save(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("un evento de un proceso ajeno se registra y se confirma")
    void unEventoDeUnProcesoAjenoSeRegistra() {
        doThrow(new UnknownReservationProcessException("process-ajeno"))
                .when(processEvent).process(any());

        listener.onCatedraEvent(mensaje(eventoValido()), acknowledgment);

        verify(failedEvents).save(anyString(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("un error recuperable no confirma el offset: el mensaje vuelve a llegar")
    void unErrorRecuperableNoConfirmaElOffset() {
        doThrow(new DataAccessResourceFailureException("la base esta caida"))
                .when(processEvent).process(any());

        var error = catchThrowableOfType(
                DataAccessResourceFailureException.class,
                () -> listener.onCatedraEvent(mensaje(eventoValido()), acknowledgment));

        assertThat(error).isNotNull();
        // El offset no se confirma: mientras el problema siga, el mensaje tiene que volver.
        verify(acknowledgment, never()).acknowledge();
        verify(failedEvents, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("traduce el evento de la catedra al hecho del negocio")
    void traduceElEvento() {
        listener.onCatedraEvent(mensaje(eventoValido()), acknowledgment);

        var captor = ArgumentCaptor.forClass(ReservationEventEnvelope.class);
        verify(processEvent).process(captor.capture());

        assertThat(captor.getValue().event()).isEqualTo(ReservationEvent.CONFIRMED);
        assertThat(captor.getValue().eventId()).isEqualTo("evento-1");
        assertThat(captor.getValue().reservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(captor.getValue().schemaVersion()).isEqualTo(1);
    }
}
