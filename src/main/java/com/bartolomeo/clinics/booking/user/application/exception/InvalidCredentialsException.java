package com.bartolomeo.clinics.booking.user.application.exception;
import com.bartolomeo.clinics.booking.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.application.exception.ErrorCode;


/**
 * Las credenciales no coinciden.
 */
public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException(String login) {
        super(ErrorCode.INVALID_CREDENTIALS, "Las credenciales no coinciden: " + login);
    }
}
