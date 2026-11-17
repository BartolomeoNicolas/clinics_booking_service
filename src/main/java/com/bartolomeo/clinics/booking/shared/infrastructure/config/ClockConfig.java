package com.bartolomeo.clinics.booking.shared.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj del servicio.
 *
 * <p><strong>Es un bean, no un {@code Instant.now()} escrito en el código.</strong> "Ahora" es
 * una decisión que el negocio necesita poder cambiar —para probar si un retén venció, para
 * fechar un proceso— y un {@code Instant.now()} en el medio de un método la vuelve imposible:
 * el test tiene que confiar en la hora de la máquina.
 *
 * <p>El reloj es de UTC porque las fechas se guardan con zona horaria y las comparaciones
 * tienen que ser las mismas en todos lados. Las fechas locales —el día del turno, la hora de
 * inicio— se manejan aparte, en {@code LocalDate} y {@code LocalTime}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
