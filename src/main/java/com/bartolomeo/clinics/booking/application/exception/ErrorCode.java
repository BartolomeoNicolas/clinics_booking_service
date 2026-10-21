package com.bartolomeo.clinics.booking.application.exception;

/**
 * Identidad estable de cada tipo de error del contexto de usuarios.
 *
 * <p>El codigo identifica <em>que paso</em>, y eso lo define el negocio. El status HTTP con el
 * que se responde es una decision de infraestructura, y no puede vivificar el dominio hacia
 * adentro: {@code application} no depende de {@code infrastructure}, nunca al reves.
 *
 * <p>El valor de {@link #getCode()} es contrato. El anexo §13 del contrato con la catedra dice
 * que los clientes deciden por {@code status} + {@code code} y nunca por el texto de
 * {@code detail}. Para que eso sea cierto, el mismo tipo de fallo tiene que devolver siempre
 * el mismo codigo.
 */
public enum ErrorCode {

    VALIDATION_ERROR("validation-error", "Solicitud inválida"),
    UNAUTHENTICATED("unauthenticated", "No autenticado"),
    TOKEN_EXPIRED("token-expired", "Token expirado"),
    FORBIDDEN("forbidden", "Sin permiso"),
    LOGIN_ALREADY_TAKEN("login-already-taken", "Login ya registrado"),
    EMAIL_ALREADY_TAKEN("email-already-taken", "Email ya registrado"),
    INVALID_CREDENTIALS("invalid-credentials", "Credenciales incorrectas"),
    USER_NOT_FOUND("user-not-found", "Usuario no encontrado"),
    NOT_FOUND("not-found", "No encontrado"),
    INTERNAL_ERROR("internal-error", "Error interno");

    private final String code;
    private final String title;

    ErrorCode(String code, String title) {
        this.code = code;
        this.title = title;
    }

    /** Cadena estable que viaja en el JSON. No cambiar sin actualizar el contrato. */
    public String getCode() {
        return code;
    }

    /** Texto corto y estable. No es el mensaje del error: ese va en `detail`. */
    public String getTitle() {
        return title;
    }

    /** URI de documentacion del codigo, para que un cliente pueda consultarla. */
    public String getType() {
        return "https://dqmdz-um.github.io/prog2/clinics/problems/" + code;
    }
}
