package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.util.Objects;

/**
 * Cómo se llama un paciente del lado de la cátedra.
 *
 * <p><strong>Es un dato derivado, no un dato que se mande.</strong> La cátedra llama
 * {@code externalPatientId} al identificador del paciente externo —en su ejemplo,
 * {@code user-42}— y este servicio lo arma con el usuario local: {@code user-{id}}. Que el
 * cliente pueda mandarlo sería un agujero: cualquiera podría reservar a nombre de otro.
 *
 * <p><strong>El formato vive en un solo lugar.</strong> Es la forma en que la cátedra ve a
 * nuestros pacientes, y si mañana cambia, cambia acá y en ningún otro lado. La regla del
 * contrato —no vacío, hasta 100 caracteres— se comprueba al construirlo, porque después de
 * armado no se vuelve a tocar.
 *
 * @param value la forma que ve la cátedra
 */
public record ExternalPatientId(String value) {

    /** Máximo que acepta la cátedra. */
    public static final int MAX_LENGTH = 100;

    /**
     * El identificador del paciente que corresponde a un usuario de este servicio.
     *
     * @param userId identificador del usuario local
     * @return el identificador en la forma de la cátedra
     * @throws IllegalArgumentException si el usuario no tiene identificador
     */
    public static ExternalPatientId ofUser(Long userId) {
        Objects.requireNonNull(userId, "el usuario del proceso de reserva no tiene identificador");
        return new ExternalPatientId("user-" + userId);
    }

    /**
     * @throws IllegalArgumentException si el identificador está vacío o es más largo de lo que
     *                                  acepta la cátedra
     */
    public ExternalPatientId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("el identificador del paciente no puede estar vacío");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "el identificador del paciente no puede pasar de " + MAX_LENGTH + " caracteres");
        }
    }
}
