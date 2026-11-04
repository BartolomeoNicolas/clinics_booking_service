package com.bartolomeo.clinics.booking.catedra.application;

/**
 * Capa de aplicación del contexto {@code catedra}: las excepciones con las que este servicio
 * nombra lo que le pasa cuando habla con la cátedra.
 *
 * <p><strong>Acá no hay casos de uso por la misma razón que en {@code catalog}:</strong> llamar
 * a otro servicio no es una regla de negocio, es una traducción. Los casos de uso que usan el
 * puerto viven en {@code availability} y en {@code reservation}.
 *
 * <p>Lo que sí está acá es la parte más importante de este contexto: <strong>cada error que la
 * cátedra puede mandar, traducido a una excepción con nombre propio y con un código estable de
 * este servicio.</strong> La cátedra decide por {@code status} + {@code code} y nunca por el
 * texto de {@code detail}; este servicio hace lo mismo con lo que le devuelve, y por eso la
 * traducción es una lista cerrada con una fila por código y no un {@code switch} que copia el
 * mensaje de afuera.
 *
 * <p>La tabla completa —código de la cátedra → excepción → código de booking → status— está en
 * el {@code README}, y hay un test por cada fila que este servicio sí puede recibir.
 */
