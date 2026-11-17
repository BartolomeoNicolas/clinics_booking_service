package com.bartolomeo.clinics.booking.catedra.domain;

/**
 * Dominio del contexto {@code catedra}: lo que este servicio entiende de la cátedra.
 *
 * <p>Es el segundo de los dos contextos anti-corrupción de booking, y el espejo de
 * {@code catalog}. Allí vive lo que se lee del servicio de catálogo; acá, lo que se pide y se
 * recibe del servicio central: ocupaciones, holds, confirmaciones, listados y cancelaciones.
 *
 * <p>La misma regla que en {@code catalog}: <strong>ni entidades, ni tablas, ni
 * repositories.</strong> La cátedra es quien es dueña de las reservas; este servicio solo
 * tiene su proceso local y lo que necesita para hablar con ella.
 *
 * <p>Vive como contexto propio porque lo consumen {@code availability} —necesita las
 * ocupaciones— y {@code reservation} —necesita los holds, la confirmación, el listado y la
 * cancelación—. El puerto en uno obligaría al otro a importarlo.
 */
