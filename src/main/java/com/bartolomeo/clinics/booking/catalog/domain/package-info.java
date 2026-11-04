package com.bartolomeo.clinics.booking.catalog.domain;

/**
 * Dominio del contexto {@code catalog}: lo que booking necesita saber del catálogo.
 *
 * <p><strong>Este contexto es una capa anti-corrupción, no una réplica.</strong> El enunciado
 * prohíbe que el servicio de turnos replique el catálogo (PLAN H4), así que acá no hay
 * entidades, ni repositories, ni tablas: solo los pocos datos descriptivos que se leen del
 * servicio de catálogo para tomar una decisión, modelados como los usa este servicio y no
 * como los devuelve aquél.
 *
 * <p>Vive como contexto propio —y no dentro de {@code availability} o {@code reservation}—
 * porque lo consumen los dos. Meterlo en uno obligaría al otro a importarlo, que es la flecha
 * que la separación de contexts existe para evitar.
 *
 * @see com.bartolomeo.clinics.booking.catalog.domain.ports.out.CatalogQueryPort
 */
