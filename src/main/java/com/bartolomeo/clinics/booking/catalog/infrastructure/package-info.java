package com.bartolomeo.clinics.booking.catalog.infrastructure;

/**
 * Infraestructura del contexto {@code catalog}: toda la tecnología vive acá.
 *
 * <ul>
 *   <li>{@code config/}: propiedades del cliente —dónde está el servicio y cuánto se espera—.</li>
 *   <li>{@code rest/}: el adaptador que implementa el puerto {@code out}, sus DTO de respuesta
 *       y el mapper que los traduce al modelo de dominio.</li>
 *   <li>{@code security/}: la emisión del JWT técnico de servicio con el que se llama al
 *       catálogo.</li>
 * </ul>
 *
 * <p><strong>No hay {@code persistence/}, y no lo va a haber.</strong> Este servicio no
 * almacena nada del catálogo: leer una agenda no la convierte en un registro propio. Si
 * algún día aparece una tabla de catálogo acá, es una violación del enunciado y no una
 * evolución del diseño.
 */
