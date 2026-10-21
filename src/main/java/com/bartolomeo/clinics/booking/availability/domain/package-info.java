package com.bartolomeo.clinics.booking.availability.domain;

/**
 * Dominio del contexto {@code availability}: cálculo de slots disponibles.
 *
 * <p><strong>Sin Spring, sin JPA.</strong> Solo modelo y puertos.
 *
 * <p>Combina los horarios semanales del catálogo con las ocupaciones informadas por la
 * cátedra. La regla de cálculo es lógica de negocio pura: no sabe de REST ni de Redis.
 */
