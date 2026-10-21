package com.bartolomeo.clinics.booking.user.application.exception;
import com.bartolomeo.clinics.booking.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.application.exception.ErrorCode;


/**
 * No existe un usuario con ese login.
 */
public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(String login) {
        super(ErrorCode.USER_NOT_FOUND, "No existe un usuario con ese login: " + login);
    }
}
