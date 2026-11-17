package com.bartolomeo.clinics.booking.reservation.domain.model;

/**
 * Los estados por los que pasa un proceso de reserva.
 *
 * <p><strong>Son los del contrato, ni uno más ni uno menos.</strong> La cátedra es la que
 * publica los hechos —pide el teléfono, confirma, rechaza, da por vencido el proceso— y este
 * servicio guarda su propia copia de cómo va. Que los nombres coincidan no es casualidad: el
 * evento de Kafka que llega trae un estado que hay que poder comparar con el nuestro sin
 * traducciones intermedias.
 *
 * <p>El recorrido normal es este:
 *
 * <pre>
 *   INITIATED -&gt; HOLD_CREATED -&gt; WAITING_FOR_PHONE -&gt; PHONE_SUBMITTED -&gt; CONFIRMED
 * </pre>
 *
 * <p>Y de ahí se sale por cuatro lados: el teléfono se rechaza y el proceso queda
 * <em>reintentable</em> hasta que venza ({@code REJECTED}), el usuario lo cancela
 * ({@code CANCELLED}), se passa la vigencia ({@code EXPIRED}) o la cátedra lo da por
 * inválido ({@code INVALID}). {@code FAILED} es el lado nuestro: el flujo no pudo completarse
 * y no tiene sentido seguir.
 *
 * <p><strong>La tabla de transiciones válidas no está acá.</strong> Qué estados pueden seguir a
 * cuáles es una regla con pruebas exhaustivas, y vive en su propio lugar para que se pueda
 * leer entera de un tirón.
 */
public enum ReservationState {

    /**
     * El proceso existe localmente y todavía no se le pidió nada a la cátedra.
     *
     * <p>Existe antes de la primera llamada, y por eso existe: si esa llamada se pierde, queda
     * un proceso al que reconciliar en vez de un hueco sin rastro.
     */
    INITIATED,

    /**
     * La cátedra aceptó el retén y devolvió su {@code holdId} y su vencimiento.
     *
     * <p>El retén está tomado del lado de la cátedra y todavía no es una reserva: falta la
     * confirmación inicial.
     */
    HOLD_CREATED,

    /**
     * La cátedra confirmó el retén y creó la reserva en estado {@code PHONE_PENDING}.
     *
     * <p>Es el estado en el que el proceso espera que el usuario mande su teléfono. La
     * confirmación inicial ya pasó.
     */
    WAITING_FOR_PHONE,

    /**
     * El usuario mandó el teléfono y se publicó al topic que la cátedra escucha.
     *
     * <p>El resultado todavía no llegó: hasta que llegue, el proceso no se sabe si quedó
     * confirmado o rechazado.
     */
    PHONE_SUBMITTED,

    /**
     * La cátedra rechazó el teléfono.
     *
     * <p><strong>Es reintentable hasta que venza el proceso.</strong> El usuario puede volver a
     * mandar el teléfono mientras {@code expiresAt} no haya pasado; después ya no.
     */
    REJECTED,

    /** La reserva quedó confirmada. Estado final. */
    CONFIRMED,

    /** El usuario canceló la reserva. Estado final. */
    CANCELLED,

    /** El proceso venció antes de completarse. Estado final. */
    EXPIRED,

    /** La cátedra dio el proceso por inválido. Estado final. */
    INVALID,

    /**
     * El flujo no pudo completarse y no tiene sentido seguir.
     *
     * <p>Es el estado en que deja un proceso al que la cátedra no dejó nada tomado: un turno
     * que ya estaba ocupado, un profesional deshabilitado, un retén que no se pudo reconciliar
     * después de un timeout. Estado final.
     */
    FAILED
}
