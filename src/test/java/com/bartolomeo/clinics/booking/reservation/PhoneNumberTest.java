package com.bartolomeo.clinics.booking.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bartolomeo.clinics.booking.reservation.domain.model.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El formato del teléfono.
 *
 * <p>La normalización es la de la cátedra —espacios, guiones, paréntesis y puntos— y el
 * formato es el que el contrato fija. Si acá se aceptara algo que allá se rechaza, el error
 * aparecería después de publicar, cuando el mensaje ya no se puede arreglar.
 */
@DisplayName("el telefono del usuario")
class PhoneNumberTest {

    @Test
    @DisplayName("quita espacios, guiones, parentesis y puntos")
    void normaliza() {
        assertThat(PhoneNumber.of("+54 9 261-555-1234").orElseThrow().value())
                .isEqualTo("+5492615551234");
        assertThat(PhoneNumber.of("(11) 1234-5678").orElseThrow().value())
                .isEqualTo("1112345678");
    }

    @Test
    @DisplayName("acepta de siete a quince digitos con un mas opcional")
    void aceptaElFormatoDelContrato() {
        assertThat(PhoneNumber.of("1234567").orElseThrow().value()).isEqualTo("1234567");
        assertThat(PhoneNumber.of("+123456789012345").orElseThrow().value())
                .isEqualTo("+123456789012345");
    }

    @Test
    @DisplayName("rechaza los que no cumplen el formato")
    void rechazaLoQueNoCumple() {
        assertThat(PhoneNumber.of("123456")).isEmpty();          // menos de siete
        assertThat(PhoneNumber.of("1234567890123456")).isEmpty(); // mas de quince
        assertThat(PhoneNumber.of("1234567a")).isEmpty();         // letras
        assertThat(PhoneNumber.of("++1234567")).isEmpty();        // dos signos mas
        assertThat(PhoneNumber.of("")).isEmpty();
        assertThat(PhoneNumber.of(null)).isEmpty();
    }

    @Test
    @DisplayName("un mas en el medio no se acepta")
    void unMasEnElMedioNoSeAcepta() {
        assertThat(PhoneNumber.of("1234+567890")).isEmpty();
    }
}
