package com.bartolomeo.clinics.booking.availability.infrastructure.web.controller;

import com.bartolomeo.clinics.booking.availability.application.service.AvailabilityService;
import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.availability.infrastructure.web.dto.AvailabilityRequest;
import com.bartolomeo.clinics.booking.availability.infrastructure.web.dto.SlotResponse;
import com.bartolomeo.clinics.booking.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/professionals/{id}/availability?from=&to=}: qué turnos quedan libres.
 *
 * <p><strong>Delgado a propósito.</strong> Toma el pedido, lo delega entero a la fachada y
 * pasa la respuesta por el mapper: no calcula nada, no decide nada y no atrapa ninguna
 * excepción —esas llegan al {@code @RestControllerAdvice}, que es el único que sabe qué status
 * corresponde a cada código.
 *
 * <p><strong>Una lista vacía es una respuesta.</strong> 200 con {@code slots: []} significa
 * "pregunté y no queda nada", que no es lo mismo que un error. El profesional inexistente o el
 * rango invertido ni siquiera llegan acá: los resuelve el caso de uso con su propio código.
 */
@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AvailabilityDtoMapper availabilityDtoMapper;

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<SlotResponse>> availability(
            @PathVariable("id") long professionalId,
            @Valid @ModelAttribute AvailabilityRequest request) {

        List<Slot> slots =
                availabilityService.getAvailability(professionalId, request.from(), request.to());

        return ResponseEntity.ok(
                slots.stream().map(availabilityDtoMapper::toResponse).toList());
    }
}
