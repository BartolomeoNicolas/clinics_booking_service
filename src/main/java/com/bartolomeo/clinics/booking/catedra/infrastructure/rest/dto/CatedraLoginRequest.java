package com.bartolomeo.clinics.booking.catedra.infrastructure.rest.dto;

/**
 * Credenciales de la cuenta técnica para pedir un JWT.
 *
 * <p><strong>Este es el único registro de booking que lleva una contraseña</strong>, y su
 * regla es la misma que la de toda credencial: no se registra en ningún log, no aparece en
 * ninguna respuesta y no se guarda en ninguna caché. Lo que se cachea es el token que devuelve
 * la respuesta, no esto.
 *
 * <p>{@code rememberMe} va en {@code true} siempre. El contrato dice que la vigencia larga
 * sale de ahí, y un JWT técnico que se renueva en el minuto de una reserva a medias es peor
 * que uno que dura: el token tiene que seguir vivo mientras el proceso corre, que es hasta que
 * llegue el teléfono.
 *
 * @param username   usuario de la cuenta técnica
 * @param password   contraseña de la cuenta técnica
 * @param rememberMe siempre {@code true}, para la vigencia larga
 */
public record CatedraLoginRequest(String username, String password, boolean rememberMe) {

    public static CatedraLoginRequest withLongSession(String username, String password) {
        return new CatedraLoginRequest(username, password, true);
    }
}
