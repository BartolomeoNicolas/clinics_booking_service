package com.bartolomeo.clinics.booking.availability.infrastructure;

/**
 * Infraestructura del contexto {@code availability}: web.
 *
 * <p>El endpoint de consulta con su DTO de entrada, sus DTOs de salida y el mapper. No hay
 * persistencia ni seguridad propias: la disponibilidad se calcula en memoria con lo que traen
 * otras dos fuentes y no guarda nada, y el endpoint ya queda detrás de la autenticación que
 * define {@code shared}.
 */
