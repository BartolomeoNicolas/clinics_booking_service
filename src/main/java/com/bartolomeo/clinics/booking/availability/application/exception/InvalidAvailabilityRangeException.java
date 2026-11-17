package com.bartolomeo.clinics.booking.availability.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
import java.time.LocalDate;

/**
 * El rango consultado está invertido: el primer día es posterior al último.
 *
 * <p><strong>Es un error del pedido y no una lista vacía.</strong> Un rango invertido no tiene
 * ningún día, y responder "no hay turnos" diría que miró la agenda y no encontró nada, cuando
 * en realidad nunca la miró. El contrato de la cátedra lo dice igual para su propio
 * endpoint —{@code to} no puede ser anterior a {@code from}— y acá se trata igual: código
 * estable {@code validation-error} con status 400, para que quien mandó el pedido sepa que
 * tiene que corregirlo y no que no quedaron lugares.
 *
 * <p>No es una validación de formato —eso lo resuelve la anotación en el DTO y llega como
 * error de campo—, es un formato válido con un significado inválido.
 */
public class InvalidAvailabilityRangeException extends ApplicationException {

    public InvalidAvailabilityRangeException(LocalDate from, LocalDate to) {
        super(ErrorCode.VALIDATION_ERROR,
                "El rango de fechas no es válido: " + from + " es posterior a " + to + ".");
    }
}
