package com.bartolomeo.clinics.booking.reservation.infrastructure;

/**
 * Infraestructura del contexto {@code reservation}: persistencia y web.
 *
 * <p>La tabla de procesos y el endpoint que los inicia. No hay seguridad propia: los
 * procesos son del usuario que los inició y esa regla se cumple en la capa de aplicación,
 * mirando siempre por el usuario de la sesión.
 */
