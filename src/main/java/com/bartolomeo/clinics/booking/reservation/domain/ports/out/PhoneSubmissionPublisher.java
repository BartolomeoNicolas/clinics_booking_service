package com.bartolomeo.clinics.booking.reservation.domain.ports.out;

import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneSubmission;

/**
 * Publica la respuesta con el teléfono.
 *
 * <p><strong>Es el único camino de salida hacia el topic de la cátedra.</strong> Del otro
 * lado está Kafka, el topic y el formato del mensaje; de este lado, un teléfono y los
 * identificadores del proceso.
 *
 * <p><strong>Publicar no es confirmar.</strong> Que el mensaje haya salido no dice que la
 * cátedra lo haya aceptado: el resultado llega después por Kafka, y es el que decide si la
 * reserva quedó confirmada o si el teléfono fue rechazado.
 */
public interface PhoneSubmissionPublisher {

    /**
     * Publica el teléfono del proceso.
     *
     * @param submission lo que hay que mandar
     */
    void publish(PhoneSubmission submission);
}
