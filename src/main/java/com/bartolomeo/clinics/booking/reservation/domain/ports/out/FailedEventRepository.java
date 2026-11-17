package com.bartolomeo.clinics.booking.reservation.domain.ports.out;

/**
 * Los eventos que no se pudieron aplicar y no tienen sentido reintentar.
 *
 * <p><strong>Es un registro de diagnóstico, no una cola.</strong> Lo que queda acá ya se
 * confirmou con la cátedra: el mensaje no se puede entender, es de un proceso ajeno, o llegó
 * a un proceso que ya estaba cerrado. Se guardan para poder mirarlos, y no se reintentan
 * solos.
 *
 * <p>Habla de eventos fallidos: no conoce entidades ni JPA.
 */
public interface FailedEventRepository {

    /**
     * Anota el evento como irrecuperable.
     *
     * @param eventId      identificador del evento
     * @param eventType    tipo del evento, tal como lo manda la cátedra
     * @param errorMessage qué pasó, en palabras que alguien pueda leer
     */
    void save(String eventId, String eventType, String errorMessage);
}
