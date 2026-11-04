package com.bartolomeo.clinics.booking.availability.infrastructure.web.mapper;

import com.bartolomeo.clinics.booking.availability.domain.model.Slot;
import com.bartolomeo.clinics.booking.availability.infrastructure.web.dto.SlotResponse;
import org.springframework.stereotype.Component;

/**
 * De turno de dominio a turno de la respuesta.
 *
 * <p>Es una copia de tres campos hoy, y por eso mismo es el lugar exacto donde va a doler el
 * día que deje de serlo: si el dominio cambia, cambia acá y no en el controller, y si cambia
 * la forma que se le promete al cliente, cambia en el DTO y no en el dominio.
 */
@Component
public class AvailabilityDtoMapper {

    /**
     * @param slot turno calculado
     * @return la misma posición en la forma que sale por HTTP, o {@code null} si entra nulo
     */
    public SlotResponse toResponse(Slot slot) {
        if (slot == null) {
            return null;
        }
        return new SlotResponse(slot.date(), slot.startTime(), slot.endTime());
    }
}
