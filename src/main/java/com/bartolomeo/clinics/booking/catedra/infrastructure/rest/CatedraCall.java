package com.bartolomeo.clinics.booking.catedra.infrastructure.rest;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Qué le estaba pidiendo a la cátedra cuando contestó mal.
 *
 * <p><strong>Existe para que el error tenga un mensaje con cosas adentro.</strong> Sin esto,
 * traducir {@code HOLD_EXPIRED} obligaría a decir "el recurso solicitado venció", que es una
 * frase con la que nadie puede empezar a investigar. Con esto se dice "el retén hold-6c0c2f41
 * venció", que ya es un lugar donde mirar.
 *
 * <p><strong>No es una decisión de negocio y no sale de la infraestructura.</strong> Vive acá,
 * al lado del traductor, y los identificadores que trae se pasan como texto a las excepciones
 * del dominio: esos identificadores ya existen en la cátedra, no los inventó este contexto, y
 * un registro de infraestructura no es el lugar donde se decide qué significa reservar.
 *
 * <p>Cada operación llena solo lo que conoce —crear conoce el profesional y el turno,
 * confirmar solo el retén—, y el traductor usa lo que haya. Que falte un dato no es un error:
 * es que la operación que se estaba haciendo no lo necesitaba para pedirlo.
 *
 * @param operation           nombre de la operación, para el mensaje y el log
 * @param professionalId      profesional del pedido, si la operación lo conoce
 * @param date                día del turno pedido, si la operación lo conoce
 * @param startTime           hora del turno pedido, si la operación lo conoce
 * @param holdId              retén en juego, si la operación lo conoce
 * @param reservationProcessId proceso en juego, si la operación lo conoce
 */
public record CatedraCall(
        String operation,
        Long professionalId,
        LocalDate date,
        LocalTime startTime,
        String holdId,
        String reservationProcessId) {

    public static CatedraCall ocupaciones(long professionalId) {
        return new CatedraCall("ocupaciones", professionalId, null, null, null, null);
    }

    public static CatedraCall crearReten(long professionalId, LocalDate date, LocalTime startTime) {
        return new CatedraCall("crear retén", professionalId, date, startTime, null, null);
    }

    public static CatedraCall confirmarReten(String holdId) {
        return new CatedraCall("confirmar retén", null, null, null, holdId, null);
    }

    public static CatedraCall listarReservas() {
        return new CatedraCall("listar reservas", null, null, null, null, null);
    }

    public static CatedraCall cancelar(String reservationProcessId) {
        return new CatedraCall("cancelar reserva", null, null, null, null, reservationProcessId);
    }

    /**
     * Cómo nombrar al profesional en un mensaje.
     *
     * <p>Al crear se conoce su id; al confirmar, no, y lo único que lo señala es el retén. Por
     * eso el resultado puede ser una frase y no siempre un número, y por eso la frase es
     * "de este retén" y no un id inventado: un error inventado es peor que uno impreciso.
     */
    public String professionalRef() {
        if (professionalId != null) {
            return String.valueOf(professionalId);
        }
        if (holdId != null) {
            return "de este retén";
        }
        return "consultado";
    }

    /** El turno en juego como lo pone el mensaje, o una frase si esta operación no lo trae. */
    public String turnoRef() {
        if (date != null && startTime != null) {
            return date + " a las " + startTime;
        }
        return "pedido";
    }
}
