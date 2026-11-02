package com.bartolomeo.clinics.booking.shared.infrastructure;

/**
 * Infraestructura de la aplicación entera: una sola por servicio.
 *
 * <ul>
 *   <li>{@code config/}: {@code LocalProperties} y {@code RequiredSecretsInitializer}, que
 *       validan los secretos al arrancar antes de que cualquier contexto los necesite.</li>
 *   <li>{@code security/}: la cadena de filtros, la validación del JWT, el par de claves RSA y
 *       los handlers que responden 401/403 en problem+json. Protege los endpoints de todos los
 *       contexts.</li>
 *   <li>{@code web/exception/}: {@code @RestControllerAdvice}, el body problem+json y el
 *       mapper de code a status. Sin esto cada controller resolvería el error a su manera.</li>
 * </ul>
 *
 * <p>El par de claves vive acá y no en {@code user/} aunque {@code user} sea quien firma: la
 * clave pública la necesitan todos los contexts para verificar el token, así que ponerlo en
 * {@code user} obligaría a que {@code shared} dependa de {@code user}, que es la flecha
 * prohibida. Lo que firma con ella, {@code JwtTokenIssuerAdapter}, sí va en {@code user}.
 *
 * <p>Lo que sí es de un contexto no viene acá: los controllers, DTOs y la persistencia de
 * identidad están en {@code user/infrastructure/}.
 */
