package com.bartolomeo.clinics.booking.catedra.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración del cliente de la cátedra: dónde está, con qué cuenta y cuánto se espera.
 *
 * <p><strong>Las credenciales no son obligatorias al arrancar.</strong> La cuenta técnica se
 * registra una sola vez y el token puede venir de afuera o de un login posterior, así que un
 * servicio sin credenciales todavía tiene que poder levantar —lo contrario rompería todos los
 * tests y cualquier arranque previo al alta—. Lo que sí es obligatorio es la URL: sin ella no
 * hay a dónde apuntar y el error de "URL vacía" no dice nada. Si faltan las credenciales en el
 * momento de usarlas, el error lo nombra en ese momento y no al arrancar.
 *
 * <p><strong>Un timeout por operación, y no uno global.</strong> Leer ocupaciones es una
 * consulta corta que la app espera con la pantalla abierta; confirmar un retén pone en marcha
 * un proceso que después se completa por Kafka, y un timeout agresivo ahí dejaría un retén
 * retenido del lado de la cátedra sin que nadie sepa si se confirmó. Valores distintos para
 * cosas distintas, y no un número mágico compartido.
 *
 * <p><strong>Sin {@code groupId}.</strong> El contrato es tajante: ninguna operación acepta un
 * {@code groupId} para elegir otra integración; la integración sale del JWT. La única razón
 * para tenerlo acá sería nombrar topics de Kafka, que llega con su issue.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "clinics.catedra")
public class CatedraProperties {

    /** Base del REST de la cátedra, por ejemplo {@code http://172.17.201.1:8090}. */
    @NotBlank(message = "clinics.catedra.base-url es obligatorio")
    private String baseUrl;

    /**
     * Usuario de la cuenta técnica.
     *
     * <p>Vacío hasta que se registre la cuenta. Nunca se registra en logs ni aparece en
     * respuestas.
     */
    private String technicalUser;

    /**
     * Contraseña de la cuenta técnica.
     *
     * <p>Vacía hasta que se registre la cuenta. No se versiona, no se registra en logs y no
     * cruza nunca hacia la app.
     */
    private String technicalPassword;

    /**
     * JWT técnico de la cátedra, si se guardó al registrar la cuenta.
     *
     * <p>Es opcional por lo mismo que las credenciales: si está y alcanza, ahorra un login; si
     * no está o venció, el emisor pide uno nuevo. El token renovado se queda solo en memoria y
     * nunca se escribe de vuelta en el almacén de secretos.
     */
    private String technicalJwt;

    /** Timeout de conexión en milisegundos, común a todas las operaciones. */
    @Min(value = 1, message = "clinics.catedra.connect-timeout-ms debe ser mayor que cero")
    private int connectTimeoutMs = 2_000;

    /** Lectura de ocupaciones. Es lo que espera el usuario con la pantalla abierta. */
    @Min(value = 1)
    @Max(value = 60_000)
    private int occupanciesReadTimeoutMs = 3_000;

    /** Lectura del listado de reservas. Puede traer varias páginas. */
    @Min(value = 1)
    @Max(value = 60_000)
    private int appointmentsReadTimeoutMs = 5_000;

    /** Crear un retén. Escribes con efectos: se le da aire antes de declarar timeout. */
    @Min(value = 1)
    @Max(value = 60_000)
    private int createHoldTimeoutMs = 5_000;

    /**
     * Confirmar un retén. La más larga de todas, y con razón: pone en marcha la creación de la
     * reserva y la publicación del pedido de teléfono, y un timeout acá deja la duda más cara
     * del sistema —¿se confirmó o no—, que después se resuelve reconciliando.
     */
    @Min(value = 1)
    @Max(value = 60_000)
    private int confirmHoldTimeoutMs = 8_000;

    /** Cancelar una reserva. */
    @Min(value = 1)
    @Max(value = 60_000)
    private int cancelTimeoutMs = 5_000;

    /**
     * Intentos extra para las dos lecturas, además del primero.
     *
     * <p>Cero significa "un solo intento", que es lo que conviene si el reintento no está
     * querido. Nunca se aplica a una operación con efectos: ahí un timeout no prueba que haya
     * fallado (anexo 18.4) y repetirla a ciegas es peor que quedarse tranquilo.
     */
    @Min(value = 0)
    @Max(value = 5)
    private int readRetries = 2;

    /**
     * Indica si ya se puede llamar a la cátedra.
     *
     * <p>Mientras sea {@code false}, el adaptador falla con un mensaje que nombra la falta, en
     * vez de intentar autenticarse con credenciales vacías y devolver un 401 engañoso.
     */
    public boolean isProvisioned() {
        return hasText(technicalUser) && hasText(technicalPassword);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
