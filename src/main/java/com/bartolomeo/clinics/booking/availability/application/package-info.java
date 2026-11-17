package com.bartolomeo.clinics.booking.availability.application;

/**
 * Capa de aplicación del contexto {@code availability}: casos de uso, fachada y excepciones.
 *
 * <p>Acá viven el orden en que se juntan las fuentes —catálogo, cátedra, fecha pedida— y el
 * reloj con el que se juzga la vigencia de los reténes. La regla pura de cálculo no: esa está
 * en {@code domain}, sin Spring y sin nada que opinar sobre de dónde vienen los datos.
 */
