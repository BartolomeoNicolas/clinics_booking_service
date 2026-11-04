package com.bartolomeo.clinics.booking.reservation.infrastructure.web.mapper;

import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationData;
import com.bartolomeo.clinics.booking.reservation.domain.model.ReservationProcess;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto.CreateReservationRequest;
import com.bartolomeo.clinics.booking.reservation.infrastructure.web.dto.ReservationProcessResponse;
import org.springframework.stereotype.Component;

/**
 * Del turno que pidió el usuario al proceso que se guarda, y del proceso a la respuesta.
 *
 * <p><strong>La clave de idempotencia entra por acá.</strong> Viene en un header y es cosa
 * del transporte, pero es una regla del negocio —sin ella un reintento crea dos procesos— y
 * por eso entra al dominio como parte de la solicitud, no como un detalle que el controller
 * se queda.
 */
@Component
public class ReservationDtoMapper {

    /**
     * @param request    turno pedido
     * @param requestKey clave de idempotencia del header; puede venir nula y la valida el
     *                   caso de uso
     * @return los datos que el dominio entiende
     */
    public ReservationData toDomain(CreateReservationRequest request, String requestKey) {
        if (request == null) {
            return null;
        }
        return new ReservationData(
                request.professionalId(),
                request.date(),
                request.startTime(),
                requestKey);
    }

    /**
     * @param process proceso de dominio
     * @return la respuesta de HTTP, o {@code null} si el proceso es nulo
     */
    public ReservationProcessResponse toResponse(ReservationProcess process) {
        if (process == null) {
            return null;
        }
        return new ReservationProcessResponse(
                process.id() == null ? 0L : process.id(),
                process.state() == null ? null : process.state().name(),
                process.professionalId(),
                process.date(),
                process.startTime(),
                process.catedraProcessId(),
                process.expiresAt());
    }
}
