package com.bartolomeo.clinics.booking.catedra.domain.model;

import java.time.Instant;

/**
 * Lo que contesta la cátedra al confirmar un retén: el proceso quedó esperando el teléfono.
 *
 * <p><strong>Este es el punto donde empieza a correr el reloj de la reserva.</strong> La
 * cátedra crea una reserva {@code PHONE_PENDING} y publica el pedido del teléfono; el proceso
 * tiene que estar completo antes de {@code expiresAt}, y esa fecha es la que se persiste en el
 * proceso local para poder mostrar la cuenta regresiva y para que la expiración sea un evento
 * que se puede anticipar.
 *
 * <p>{@code status} queda como texto y no como enumeración a propósito: lo devuelve la cátedra
 * y lo interpreta la máquina de estados local (BOOK-M2-04), que es quien tiene el vocabulario
 * de los estados de un proceso. Que el cliente traduzca a su modelo ahora mismo sería decidir
 * en dos lugares qué significa esperar el teléfono.
 *
 * @param reservationProcessId el proceso, el identificador que dura
 * @param status                estado devuelto por la cátedra, hoy siempre {@code WAITING_FOR_PHONE}
 * @param expiresAt             límite para completar el proceso
 */
public record AppointmentConfirmation(
        String reservationProcessId,
        String status,
        Instant expiresAt) {
}
