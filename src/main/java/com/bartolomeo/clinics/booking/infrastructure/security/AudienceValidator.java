package com.bartolomeo.clinics.booking.infrastructure.security;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/**
 * Valida que el token declare la audiencia esperada.
 *
 * <p><strong>Por que hace falta ademas de la firma:</strong> un JWT validado solo por firma
 * prueba que quien lo firmo es el emisor de confianza, pero no que el token sea <em>para
 * este</em> servicio. Sin este chequeo, un token emitido legitimamente para otro servicio del
 * mismo emisor se aceptaria aca: seria una firma valida en la puerta equivocada.
 *
 * <p>Por eso el enunciado pide validar firma, vigencia <em>y</em> audiencia.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error("invalid_token",
                    "El token no declara la audiencia esperada", null);

    private final String requiredAudience;

    public AudienceValidator(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {

        List<String> audience = token.getAudience();

        if (audience == null || audience.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }

        if (!audience.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }

        return OAuth2TokenValidatorResult.success();
    }

    /**
     * Valida que el emisor sea el esperado, solo si se configuro uno.
     *
     * <p>Se separa del validador de audiencia porque es opcional: hay despliegues sin
     * {@code issuer} declarado, y en ese caso omitir el chequeo es correcto, pero dejarlo
     * implicito es peor que dejarlo explicito.
     */
    public record IssuerValidator(String requiredIssuer) implements OAuth2TokenValidator<Jwt> {

        private static final OAuth2Error INVALID_ISSUER =
                new OAuth2Error("invalid_token", "El emisor del token no es el esperado", null);

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (requiredIssuer == null || requiredIssuer.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            String issuer = token.getClaimAsString(JwtClaimNames.ISS);
            if (requiredIssuer.equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
        }
    }
}
