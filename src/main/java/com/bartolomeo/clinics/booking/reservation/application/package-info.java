package com.bartolomeo.clinics.booking.reservation.application;

/**
 * Capa de aplicación del contexto {@code reservation}: casos de uso, fachada y excepciones.
 *
 * <p>Acá viven el orden en que se pide el turno a la cátedra, la regla de idempotencia que
 * hace que un reintento no cree un segundo proceso, y la reconciliación previa a repetir
 * una escritura cuyo resultado se perdió.
 */
