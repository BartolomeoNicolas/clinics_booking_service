package com.bartolomeo.clinics.booking.user.domain;

/**
 * Dominio del contexto {@code user}: registro, autenticación y emisión del JWT de sesión.
 *
 * <p><strong>Sin Spring, sin JPA.</strong> Solo modelo y puertos.
 *
 * <p>Este contexto es el dueño de la identidad. El servicio de catálogo valida el JWT con la
 * clave pública y nunca ve contraseñas (decisión D-4 del plan maestro).
 *
 * <p>Regla verificable: ningún archivo de este paquete puede importar
 * {@code org.springframework.*} ni {@code jakarta.persistence.*}.
 */
