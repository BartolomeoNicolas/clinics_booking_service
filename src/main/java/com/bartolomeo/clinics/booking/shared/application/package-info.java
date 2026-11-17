package com.bartolomeo.clinics.booking.shared.application;

/**
 * Excepciones de aplicación compartidas por todos los contexts.
 *
 * <p>{@code ApplicationException} y {@code ErrorCode} son la base sobre la que cada contexto
 * define las suyas: los codes entran en el body problem+json y son parte del contrato, no un
 * detalle de implementación. Cada contexto cuelga sus excepciones de acá
 * ({@code user/application/exception/}), sin que {@code shared} sepa cuáles son.
 */
