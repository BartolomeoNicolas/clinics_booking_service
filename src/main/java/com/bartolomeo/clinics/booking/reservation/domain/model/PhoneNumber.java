package com.bartolomeo.clinics.booking.reservation.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Un teléfono, ya normalizado y con el formato que la cátedra acepta.
 *
 * <p><strong>La normalización es la de la cátedra, no la nuestra.</strong> El contrato dice
 * que antes de validar se eliminan espacios, guiones, paréntesis y puntos, y que el
 * resultado tiene que cumplir {@code ^\+?[0-9]{7,15}$}. Se normaliza igual que del otro
 * lado: si acá se aceptara algo que allá se rechaza, el error aparecería después de
 * publicar, cuando el mensaje ya no se puede arreglar.
 *
 * <p><strong>El teléfono no se guarda en el proceso.</strong> Es un dato del usuario que va
 * hacia la cátedra y no vuelve: lo que queda en el proceso es que se envió y cuándo, no el
 * número.
 *
 * @param value teléfono normalizado, tal como lo quiere la cátedra
 */
public record PhoneNumber(String value) {

    /** El formato que acepta la cátedra. */
    private static final Pattern VALIDO = Pattern.compile("^\\+?[0-9]{7,15}$");

    /** Lo que la cátedra elimina antes de validar. */
    private static final Pattern PARA_ELIMINAR = Pattern.compile("[\\s\\-().]");

    /**
     * Normaliza y valida un teléfono.
     *
     * @param raw teléfono como lo escribió el usuario
     * @return el teléfono normalizado, o vacío si no cumple el formato
     */
    public static Optional<PhoneNumber> of(String raw) {
        String normalizado = normalize(raw);
        if (!VALIDO.matcher(normalizado).matches()) {
            return Optional.empty();
        }
        return Optional.of(new PhoneNumber(normalizado));
    }

    /**
     * El teléfono con los espacios, guiones, paréntesis y puntos eliminados.
     *
     * @param raw teléfono como lo escribió el usuario
     * @return el teléfono sin los caracteres que la cátedra no mira
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return PARA_ELIMINAR.matcher(raw).replaceAll("");
    }
}
