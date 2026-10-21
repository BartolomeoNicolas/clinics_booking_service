package com.bartolomeo.clinics.booking.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Falla el arranque si falta un secreto obligatorio, antes de crear ningun bean.
 *
 * <p>Los inicializadores de contexto corren despues de cargar las propiedades y antes de
 * crear beans, que es exactamente lo que hace falta. Un {@code @Validated} sobre
 * {@code @ConfigurationProperties} no alcanza: la validacion corre cuando se instancia el
 * bean, y Hibernate crea su {@code EntityManagerFactory} antes, con lo que el contexto muere
 * antes de que la clase llegue a existir.
 *
 * <p>Verificado el 13/10 en el servicio de catalogo: con el secreto ausente, la app llegaba
 * hasta el driver de Postgres y moria ahi.
 */
public class RequiredSecretsInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Secretos obligatorios para arrancar, y como se documentan.
     *
     * <p>La clave es la propiedad; el valor es lo que el mensaje le muestra a quien lee el
     * error, para que sepa donde definirla.
     */
    private static final Map<String, String> REQUIRED = new LinkedHashMap<>();

    static {
        REQUIRED.put("clinics.local.db-password", "DB_PASSWORD");
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        ConfigurableEnvironment env = context.getEnvironment();

        Map<String, String> missing = new LinkedHashMap<>();
        REQUIRED.forEach((property, variable) -> {
            if (!StringUtils.hasText(env.getProperty(property))) {
                missing.put(property, variable);
            }
        });

        if (missing.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder()
                .append(System.lineSeparator())
                .append("Faltan secretos requeridos para arrancar:")
                .append(System.lineSeparator());

        missing.forEach((property, variable) -> message
                .append("  · ").append(variable)
                .append("  (propiedad '").append(property).append("')")
                .append(System.lineSeparator()));

        message.append(System.lineSeparator())
                .append("Definilos en alguno de estos lugares, ninguno versionado:")
                .append(System.lineSeparator())
                .append("  1. .env en la raiz del repo          → cp .env.example .env && chmod 600 .env")
                .append(System.lineSeparator())
                .append("  2. ~/.config/clinics/secrets/booking.yml")
                .append(System.lineSeparator())
                .append("  3. variables de entorno del sistema")
                .append(System.lineSeparator());

        throw new IllegalStateException(message.toString());
    }
}
