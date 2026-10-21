package com.bartolomeo.clinics.booking.user.application.exception;
import com.bartolomeo.clinics.booking.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.application.exception.ErrorCode;


/**
 * El login ya esta registrado.
 */
public class LoginAlreadyTakenException extends ApplicationException {

    public LoginAlreadyTakenException(String login) {
        super(ErrorCode.LOGIN_ALREADY_TAKEN, "El login ya esta registrado: " + login);
    }
}
