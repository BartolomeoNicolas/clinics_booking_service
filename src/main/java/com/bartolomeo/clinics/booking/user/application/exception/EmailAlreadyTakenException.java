package com.bartolomeo.clinics.booking.user.application.exception;
import com.bartolomeo.clinics.booking.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.application.exception.ErrorCode;


/**
 * El email ya esta registrado.
 */
public class EmailAlreadyTakenException extends ApplicationException {

    public EmailAlreadyTakenException(String email) {
        super(ErrorCode.EMAIL_ALREADY_TAKEN, "El email ya esta registrado: " + email);
    }
}
