package com.bartolomeo.clinics.booking.reservation.domain;

/**
 * Dominio del contexto {@code reservation}: procesos de reserva y su máquina de estados.
 *
 * <p><strong>Sin Spring, sin JPA.</strong> Solo modelo y puertos.
 *
 * <p>Acá viven las transiciones válidas (solicitado → bloqueado → confirmado / rechazado /
 * expirado / inválido) y la regla de aislamiento: un usuario solo ve y cancela los procesos
 * propios (enunciado §9).
 */
