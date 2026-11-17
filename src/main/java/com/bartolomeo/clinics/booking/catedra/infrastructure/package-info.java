package com.bartolomeo.clinics.booking.catedra.infrastructure;

/**
 * Infraestructura del contexto {@code catedra}.
 *
 * <ul>
 *   <li>{@code config/}: dónde está la cátedra, con qué cuenta se la llama y cuánto se espera
 *       en cada operación.</li>
 *   <li>{@code security/}: cómo se consigue y se mantiene el JWT técnico, que es credencial de
 *       este servicio y no puede salir nunca hacia la app.</li>
 *   <li>{@code rest/}: el adaptador, sus DTO del hilo de cable, el mapper y el traductor de la
 *       matriz de errores.</li>
 * </ul>
 *
 * <p><strong>No hay {@code persistence/}, igual que en {@code catalog}.</strong> Los
 * identificadores que la cátedra devuelve —{@code holdId}, {@code reservationProcessId}— se
 * persisten en {@code reservation}, que es donde vive el proceso local al que pertenecen, no
 * acá: un cliente no es dueño de nada de lo que pide prestado.
 */
