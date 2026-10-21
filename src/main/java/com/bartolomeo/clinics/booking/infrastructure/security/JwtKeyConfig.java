package com.bartolomeo.clinics.booking.infrastructure.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * El material criptografico del servicio: firmar y verificar los tokens de sesion.
 *
 * <p>El par RSA vive en el directorio de secretos y lo administra
 * {@link KeyPairProvider}: este servicio firma con la privada, y el servicio de catalogo
 * verifica con la publica.
 *
 * <p>La regla de dependencia sigue intacta: aca hay tecnologia, y la capa de aplicacion no
 * sabe nada de claves.
 */
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {

    /**
     * Firma los tokens de sesion con la clave privada de este servicio.
     *
     * <p>RS256 necesita la privada para firmar y la publica para verificar, por eso el encoder
     * usa las dos.
     */
    @Bean
    public JwtEncoder jwtEncoder(KeyPairProvider keyPairProvider) {
        return NimbusJwtEncoder
                .withKeyPair(keyPairProvider.publicKey(), keyPairProvider.privateKey())
                .build();
    }

    /**
     * Valida los tokens de sesion.
     *
     * <p>Valida por tres cosas y las tres hacen falta: la firma, la vigencia y la audiencia.
     * Con el issuer, si viene configurado.
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) throws IOException {

        RSAPublicKey publicKey = readPublicKey(Path.of(properties.getPublicKeyPath()));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new AudienceValidator(properties.getAudience()));
        validators.add(new AudienceValidator.IssuerValidator(properties.getIssuer()));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                validators.toArray(new OAuth2TokenValidator[0])));

        return decoder;
    }

    private RSAPublicKey readPublicKey(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            throw new IllegalStateException(
                    "No se pudo leer la clave publica de JWT en: " + path.toAbsolutePath()
                            + ". Definí CLINICS_JWT_PUBLIC_KEY_PATH.");
        }
        String pem = Files.readString(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "La clave publica de JWT no es una clave RSA valida: " + path, ex);
        }
    }
}
