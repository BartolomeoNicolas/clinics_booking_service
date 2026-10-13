package com.bartolomeo.clinics.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de turnos y reservas.
 *
 * <p>Bounded contexts: {@code user} (registro, login y emisión del JWT de sesión),
 * {@code availability} (cálculo de slots disponibles) y {@code reservation} (procesos,
 * reservas y su máquina de estados).
 *
 * <p>Este servicio es el dueño de la identidad de usuario: emite el JWT de sesión.
 * El servicio de catálogo solo lo valida con la clave pública (decisión D-4 del plan maestro).
 *
 * <p>La estructura sigue las convenciones del repo de referencia del profesor
 * {@code dqmdz/hexagonal}: los ports {@code in} viven en {@code domain/ports/in}, no en
 * {@code application}. Ver {@code docs/hexagonal-profesor.md}.
 */
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}