package com.bartolomeo.clinics.booking.user.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.shared.infrastructure.security.JwtProperties;
import com.bartolomeo.clinics.booking.user.domain.ports.out.TokenIssuerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

/**
 * Emite el token de sesion con RS256.
 *
 * <p>El token lleva los permisos del usuario, para que el servicio de catalogo pueda
 * autorizar sin volver a consultarlo, y su id, para que cuando este servicio le hable a
 * otro pueda llevar puesto de quien se trata sin mandarlo por un parametro que cualquiera
 * puede reescribir por el camino.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    /** Vigencia de la sesion. */
    private static final long VALIDITY_MINUTES = 60;

    @Override
    public IssuedToken issue(User user) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(VALIDITY_MINUTES, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getLogin())
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("login", user.getLogin())
                // El id viaja firmado y no como parametro suelto. Es el que despues se
                // propaga adentro del JWT tecnico cuando turnos le habla al catalogo
                // (vease ServiceTokenIssuer): un id dentro de un token con nuestra firma
                // no lo puede alterar nadie que no tenga la clave privada, mientras que
                // un id mandado en la URL lo puede reescribir quien este en el medio.
                // Y si no lo hubiera, que no lo hubiera: un claim ausente se propaga
                // ausente, pero no se rellena con cero.
                .claim("userId", user.getId())
                .claim("authorities", user.getRoles() == null
                        ? List.of()
                        : user.getRoles().stream().map(Enum::name).toList())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, expiresAt);
    }
}
