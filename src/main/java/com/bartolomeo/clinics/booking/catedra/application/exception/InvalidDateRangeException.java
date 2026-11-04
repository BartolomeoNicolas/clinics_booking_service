package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El rango de fechas pedido no puede existir: el inicio es posterior al fin.
 *
 * <p>Comparte código con cualquier validación porque, para quien llama, es lo mismo: "mandaste
 * algo mal formado, míralo de nuevo". Se le da clase propia igual, por dos motivos que no son
 * cosméticos. Primero, en este caso lo mandó <em>nuestro</em> código y no el usuario, así que
 * si aparece es un bug del lado de acá y conviene saberlo en el log sin leer una pila. Segundo,
 * la matriz de la cátedra tiene una fila con este nombre y traducirla a algo genérico haría
 * que esa fila no tuviera dónde vivir.
 */
public class InvalidDateRangeException extends ApplicationException {

    public InvalidDateRangeException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
