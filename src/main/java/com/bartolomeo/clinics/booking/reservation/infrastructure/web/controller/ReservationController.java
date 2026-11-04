package com.bartolomeo.clinics.booking.reservation.infrastructure.web.controller;

import com.bartolomeo.clinics.booking.reservation.application.service.ReservationService;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto.CreateReservationRequest;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto.ReservationProcessResponse;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto.SubmitPhoneRequest;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/reservations}: inicia un proceso de reserva.
 *
 * <p><strong>El usuario sale del token y el turno del cuerpo.</strong> El controller no mira
 * quién es: se lo pasa a la fachada tal como vino en el token, y es el caso de uso el que
 * deriva de ahí el identificador del paciente que verá la cátedra.
 *
 * <p><strong>La clave de idempotencia es un header.</strong> Va en {@code Idempotency-Key} y
 * es obligatoria: sin ella no hay forma de distinguir un reintento de una reserva nueva, y el
 * peor resultado —dos reservas para lo mismo— nace de adivinar. La que llega repetida
 * devuelve el mismo proceso, no uno nuevo.
 *
 * <p><strong>Delgado a propósito.</strong> Toma el pedido, lo delega entero a la fachada y
 * traduce el proceso a la respuesta. No atrapa excepciones: de eso se encarga el
 * {@code @RestControllerAdvice}, que es el único que sabe qué status corresponde a cada
 * código.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationDtoMapper reservationDtoMapper;

    @PostMapping
    public ResponseEntity<ReservationProcessResponse> create(
            Principal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey,
            @Valid @RequestBody CreateReservationRequest request) {

        ReservationData data = reservationDtoMapper.toDomain(request, requestKey);
        ReservationProcess process = reservationService.create(principal.getName(), data);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationDtoMapper.toResponse(process));
    }

    /**
     * Manda el teléfono de un proceso que está esperando la información.
     *
     * <p><strong>El proceso es del usuario de la sesión.</strong> No hay un parámetro que
     * diga de quién es: sale del token, y el caso de uso se encarga de que un proceso ajeno
     * se responda igual que uno que no existe.
     *
     * <p><strong>El resultado todavía no está.</strong> Lo que sale ahora es que el mensaje
     * se publicó; si la cátedra acepta el teléfono o lo rechaza llega después por Kafka, y
     * el proceso se entera por ahí.
     */
    @PostMapping("/{processId}/phone")
    public ResponseEntity<ReservationProcessResponse> submitPhone(
            Principal principal,
            @PathVariable("processId") long processId,
            @Valid @RequestBody SubmitPhoneRequest request) {

        ReservationProcess process = reservationService.submitPhone(
                processId, principal.getName(), request.phoneNumber());

        return ResponseEntity.ok(reservationDtoMapper.toResponse(process));
    }
}
