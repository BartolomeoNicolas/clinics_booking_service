package com.bartolomeo.clinics.booking.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bartolomeo.clinics.booking.user.domain.model.User;
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
 * autorizar sin volver a consultarlo.
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
