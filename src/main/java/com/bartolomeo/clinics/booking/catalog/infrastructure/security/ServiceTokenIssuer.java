package com.bartolomeo.clinics.booking.catalog.infrastructure.security;

import com.bartolomeo.clinics.booking.catalog.infrastructure.config.CatalogProperties;
import com.bartolomeo.clinics.booking.shared.infrastructure.security.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Emite el JWT técnico con el que booking llama al servicio de catálogo.
 *
 * <p><strong>Es un token de servicio, no de sesión.</strong> Mientras que el token de sesión
 * dice "esta persona está logueada", este dice "lo que viene es el servicio de turnos hablando
 * en nombre de esa persona", y lo firma con la misma clave privada con la que firma sus
 * sesiones. El catálogo verifica con la pública que tiene; si la firma no cierra, no contesta.
 *
 * <p><strong>La identidad no se manda en la URL ni en un header suelto: se propaga dentro de
 * un token firmado.</strong> Ese es el punto. Si el servicio de turnos le dijera al catálogo
 * "el usuario es el 15" en un parámetro, cualquiera que intercepte la petición podría repetirla
 * diciendo "el usuario es el 16". Firmado con la clave que solo este servicio tiene, el
 * catálogo puede creerse de quién se trata sin tener que fiarse de lo que el cliente quiso
 * poner.
 *
 * <p>Los claims que salen:
 *
 * <ul>
 *   <li>{@code sub} — quién llama: el nombre de este servicio.</li>
 *   <li>{@code iss} y {@code aud} — igualitos a los del token de sesión, para que la misma
 *       clave sirva para validar los dos sin tener que administrar dos juegos de credenciales.</li>
 *   <li>{@code scope} — los permisos del usuario, propagados desde la sesión que lo trajo
 *       hasta acá. Así el catálogo puede autorizar por rol sin volver a preguntarle a nadie.</li>
 *   <li>{@code userId} y {@code login} — quién es ese usuario.</li>
 * </ul>
 *
 * <p><strong>Se propaga lo que hay y no se inventa lo que falta.</strong> Si no hay usuario
 * en el contexto —una llamada que nace de un consumidor de Kafka y no de un pedido, por
 * ejemplo—, el token sale solo con la identidad del servicio. Un token de servicio firmado es
 * igual de válido en esos casos; lo que no se hace es rellenar un {@code userId} con cero ni
 * adivinar un login, porque un dato inventado y firmado es peor que un dato ausente.
 *
 * <p>Vive en {@code catalog} y no en {@code shared} porque codifica qué claims espera este
 * catálogo en particular. Si otro servicio exigiera otra forma, tendría el suyo.
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenIssuer {

    /** Vigencia del token técnico. */
    private static final long VALID_MINUTES = 5;

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final CatalogProperties catalogProperties;

    /**
     * El token con el que se lo llama al catálogo en este momento.
     *
     * <p>Se emite por llamada y no se cachea: es de segundos de vida, firmarlo de nuevo cuesta
     * menos que llevar la cuenta de si el que tenemos sigue siendo bueno.
     *
     * @return el token ya firmado, listo para el header {@code Authorization}
     */
    public String issue() {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(catalogProperties.getServiceName())
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .issuedAt(now)
                .expiresAt(now.plus(VALID_MINUTES, ChronoUnit.MINUTES));

        propagate(claims);

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),
                                claims.build()))
                .getTokenValue();
    }

    /**
     * Copia la identidad de la sesión entrante al token saliente, si la hay.
     *
     * <p>Lee del {@link SecurityContextHolder}, que es donde el resource server dejó el JWT que
     * trajo el usuario. No es una consulta a la base ni una llamada: es el mismo contexto que
     * ya usó la autorización de esta petición.
     */
    private void propagate(JwtClaimsSet.Builder claims) {
        Jwt session = sessionJwt();
        if (session == null) {
            return;
        }

        String login = session.getClaimAsString("login");
        if (login == null || login.isBlank()) {
            login = session.getSubject();
        }
        if (login != null && !login.isBlank()) {
            claims.claim("login", login);
        }

        Object userId = session.getClaim("userId");
        if (userId != null) {
            claims.claim("userId", userId);
        }

        List<String> scope = session.getClaimAsStringList("authorities");
        if (scope != null && !scope.isEmpty()) {
            claims.claim("scope", scope);
        }
    }

    private Jwt sessionJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
