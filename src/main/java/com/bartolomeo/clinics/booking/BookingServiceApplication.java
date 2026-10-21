package com.bartolomeo.clinics.booking;

import com.bartolomeo.clinics.booking.infrastructure.config.RequiredSecretsInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de turnos y reservas.
 *
 * <p>Bounded contexts: {@code user} (registro, login y emision del JWT de sesion),
 * {@code availability} (calculo de slots disponibles) y {@code reservation} (procesos,
 * reservas y su maquina de estados).
 *
 * <p>Este servicio es el dueno de la identidad de usuario: emite el JWT de sesion.
 * El servicio de catalogo solo lo valida con la clave publica (decision D-4 del plan maestro).
 *
 * <p>La estructura de cada contexto sigue las convenciones del repo de referencia del profesor
 * {@code dqmdz/hexagonal}: los ports {@code in} viven en {@code domain/ports/in}, no en
 * {@code application}.
 */
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {

        // Corre antes de crear beans: si falta un secreto, el arranque para aca
        // nombrando la variable, en vez de morir en el driver de la base.
        SpringApplication app = new SpringApplication(BookingServiceApplication.class);
        app.addInitializers(new RequiredSecretsInitializer());
        app.run(args);
    }
}