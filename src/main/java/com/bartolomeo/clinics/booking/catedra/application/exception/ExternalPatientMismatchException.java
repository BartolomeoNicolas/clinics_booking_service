package com.bartolomeo.clinics.booking.catedra.application.exception;

import com.bartolomeo.clinics.booking.shared.application.exception.ApplicationException;
import com.bartolomeo.clinics.booking.shared.application.exception.ErrorCode;

/**
 * El paciente con el que se intenta confirmar no es el que quedó guardado en el retén.
 *
 * <p><strong>No es un error de formato: es una coincidencia que tiene que dar.</strong> El
 * contrato exige que el {@code externalPatientId} de la confirmación coincida con el del retén,
 * y esa regla existe para que nadie pueda confirmar un turno que reservó otro. Si saltara, o
 * bien el cliente mandó un id que no leyó del retén —un bug de nuestro lado—, o bien alguien
 * está mandando identificadores que no le corresponden, y en los dos casos lo que hay que hacer
 * es parar y mirar, no reintentar.
 *
 * <p>400 de la cátedra y 400 acá: es el pedido el que no se puede cumplir tal cual vino.
 */
public class ExternalPatientMismatchException extends ApplicationException {

    public ExternalPatientMismatchException(String holdId) {
        super(ErrorCode.EXTERNAL_PATIENT_MISMATCH,
                "La confirmación del retén " + holdId
                        + " no coincide con el paciente con el que se creó.");
    }
}
