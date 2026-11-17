package com.bartolomeo.clinics.booking.user.infrastructure;

/**
 * Infraestructura del contexto {@code user}: persistencia, web y seguridad.
 *
 * <ul>
 *   <li>{@code persistence/}: {@code entity} (con anotaciones JPA), {@code repository}
 *       (Spring Data), {@code adapter} (implementa el port {@code out}) y {@code mapper}
 *       (domain ↔ entity).</li>
 *   <li>{@code web/}: {@code controller}, {@code dto} con {@code jakarta.validation} y su
 *       {@code mapper}.</li>
 *   <li>{@code security/}: los dos adapters que implementan puertos de este contexto,
 *       {@code BCryptPasswordEncoderAdapter} ({@code PasswordEncoderPort}) y
 *       {@code JwtTokenIssuerAdapter} ({@code TokenIssuerPort}).</li>
 * </ul>
 *
 * <p>La cadena de filtros, el par de claves y {@code JwtProperties} no están acá: validan los
 * endpoints de todos los contexts y viven en {@code shared/infrastructure/security}. Ponerlos
 * en {@code user} obligaría al resto a depender de este contexto.
 */
