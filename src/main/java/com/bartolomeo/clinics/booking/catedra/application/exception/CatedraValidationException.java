package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;
import java.util.List;

/**
 * La cátedra rechazó el cuerpo que mandamos por un campo, una forma o una longitud.
 *
 * <p><strong>Se traduce a nuestro código y no se reenvía el de la cátedra</strong>, porque el
 * cliente de este servicio decide por {@code status} + {@code code} y no debería tener que
 * saber qué backend contestó. Además, los campos vienen rotulados con el nombre del DTO
 * <em>del otro lado</em> —{@code appointmentHoldRequestDTO}—, que no le dicen nada a la app.
 *
 * <p>Los errores de campo se conservan igual: siguen siendo la única parte de la respuesta en
 * que un texto merece llegar al cliente, y acá ya está en nuestro idioma.
 *
 * @param fieldErrors errores por campo, con el nombre del campo y qué esperaba la validación
 */
public class CatedraValidationException extends ApplicationException {

    private final transient List<String> fieldErrors;

    public CatedraValidationException(String message, List<String> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** Campos que la cátedra rechazó. Puede venir vacío si el problema no fue de campos. */
    public List<String> getFieldErrors() {
        return fieldErrors;
    }
}
