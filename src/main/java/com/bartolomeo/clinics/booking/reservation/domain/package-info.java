package com.bartolomeo.clinics.booking.reservation.domain;

/**
 * Dominio del contexto {@code reservation}: procesos de reserva y su máquina de estados.
 *
 * <p><strong>Sin Spring, sin JPA.</strong> Solo modelo y puertos.
 *
 * <p>Acá viven los estados por los que pasa un proceso y los identificadores que la cátedra
 * da —el retén, el proceso de la cátedra, el vencimiento— con la regla de que la vigencia
 * real la determina la cátedra y una copia local no la extiende. También la forma en que
 * este servicio ve a un paciente, que se deriva del usuario local y nunca de lo que manda
 * el cliente.
 */
