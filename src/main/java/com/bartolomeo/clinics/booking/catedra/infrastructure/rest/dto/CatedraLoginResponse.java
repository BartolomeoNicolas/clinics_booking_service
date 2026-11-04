package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lo que contesta {@code POST /api/authenticate}.
 *
 * <p>Se queda solo con el token: la configuración de la integración que viene en el mismo
 * cuerpo —Redis, Kafka, namespaces— se usa en otros issues, y traerla acá sería guardar datos
 * que este contexto todavía no sabe para qué son.
 *
 * <p><strong>El componente se llama {@code id_token} y no se renombra.</strong> Es el nombre
 * exacto del contrato y renombrarlo obligaría a una anotación que alguien puede olvidar; el
 * hilo de cable se refleja tal cual, que es la regla de todos los DTO de este servicio.
 *
 * <p>{@code integration} y lo que venga después se ignoran, tolerando campos nuevos.
 *
 * @param idToken JWT técnico con el que se llaman los endpoints protegidos
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraLoginResponse(String id_token) {
}
