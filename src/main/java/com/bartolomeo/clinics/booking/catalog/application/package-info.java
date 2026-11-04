package com.bartolomeo.clinics.booking.catalog.application;

/**
 * Capa de aplicación del contexto {@code catalog}.
 *
 * <p>Hoy tiene las excepciones propias del contexto y <strong>no tiene casos de uso, y no es un
 * descuido</strong>. La issue BOOK-M1-01 entrega el contrato: el puerto {@code out} y su
 * adaptador. Quien usa ese contrato son los casos de uso de {@code availability} y de
 * {@code reservation}, que viven en sus propios contextos y llaman al puerto directamente. Un
 * caso de uso acá no tendría nada que orquestar: no hay regla de negocio propia en hablar con
 * otro servicio, hay una traducción.
 *
 * <p>Las excepciones sí están acá, y no en {@code shared}: no hablan de turnos ni de usuarios,
 * hablan de una dependencia concreta —el catálogo—, y cuando se agregue la del servicio de la
 * cátedra tendrá las suyas en su propio contexto. Ponerlas en {@code shared} mezclaría en un
 * solo lugar errores que responden a contratos distintos.
 *
 * <p>El paquete existe desde ahora para que la estructura sea la misma que en los demás
 * contexts y para que el primer caso de uso que haga falta tenga dónde caer.
 */
