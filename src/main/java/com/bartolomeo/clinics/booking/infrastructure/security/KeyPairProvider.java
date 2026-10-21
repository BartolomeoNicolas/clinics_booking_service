package com.bartolomeo.clinics.booking.infrastructure.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * El par RSA con el que se firman los JWT de sesion.
 *
 * <p>Se genera solo en el primer arranque si el archivo de la clave privada no existe, y se
 * regenera si {@code CLINICS_JWT_ROTATE=true}. Los dos archivos quedan en el directorio de
 * secretos: la privada con permisos 600 y la publica con 644, porque el servicio de catalogo
 * tiene que poder leerla.
 *
 * <p><strong>La clave privada nunca sale de esta maquina y nunca se registra.</strong> Los logs
 * dicen el <em>path</em>, nunca el contenido. La publica se puede leer libremente: no es un
 * secreto, es lo que permite verificar las firmas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyPairProvider {

    private final JwtProperties properties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    /**
     * Deja el par listo antes de que el servicio acepte una peticion.
     *
     * <p>Se ejecuta en el arranque y no en el primer uso: el servicio de catalogo tiene que
     * poder leer la clave publica en cualquier momento, y si la generacion fuera perezosa la
     * clave publica no existiria hasta que alguien pidiera un token.
     */
    @PostConstruct
    void ensureKeyPair() {
        Path privatePath = Path.of(properties.getPrivateKeyPath());
        Path publicPath = Path.of(properties.getPublicKeyPath());

        try {
            Files.createDirectories(privatePath.toAbsolutePath().getParent());
            restrictDirectoryPermissions(privatePath.toAbsolutePath().getParent());

            if (properties.isRotate() || !Files.isReadable(privatePath)) {
                generate(privatePath, publicPath);
                log.info("Par RSA generado en {} ({} bits). La clave publica esta en {}",
                        privatePath.toAbsolutePath(), properties.getKeySize(),
                        publicPath.toAbsolutePath());
                return;
            }

            this.privateKey = readPrivateKey(privatePath);
            this.publicKey = readPublicKey(publicPath);
            log.info("Par RSA cargado desde {}", privatePath.toAbsolutePath());

        } catch (IOException | GeneralSecurityException ex) {
            // El path va en el mensaje: un path mal escrito es el error mas probable, y sin el
            // es imposible de diagnosticar.
            throw new IllegalStateException(
                    "No se pudo preparar el par RSA de firma. Revisar "
                            + privatePath.toAbsolutePath() + " y " + publicPath.toAbsolutePath(),
                    ex);
        }
    }

    /** @return la clave privada con la que se firma. No se registra ni se expone. */
    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    /** @return la clave publica que el servicio de catalogo usa para verificar. */
    public RSAPublicKey publicKey() {
        return publicKey;
    }

    private void generate(Path privatePath, Path publicPath)
            throws IOException, GeneralSecurityException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(properties.getKeySize());
        KeyPair pair = generator.generateKeyPair();

        this.privateKey = (RSAPrivateKey) pair.getPrivate();
        this.publicKey = (RSAPublicKey) pair.getPublic();

        writePem(privatePath, "PRIVATE KEY", privateKey.getEncoded(), 600);
        writePem(publicPath, "PUBLIC KEY", publicKey.getEncoded(), 644);
    }

    private RSAPrivateKey readPrivateKey(Path path) throws IOException, GeneralSecurityException {
        byte[] der = readPem(path, "PRIVATE KEY");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey readPublicKey(Path path) throws IOException, GeneralSecurityException {
        byte[] der = readPem(path, "PUBLIC KEY");
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private void writePem(Path path, String type, byte[] der, int permissions) throws IOException {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem);
        restrictPermissions(path, permissions);
    }

    private byte[] readPem(Path path, String type) throws IOException {
        String pem = Files.readString(path)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }

    /**
     * Ajusta los permisos de un archivo.
     *
     * <p>La privada queda en 600: solo su dueno la lee. La publica en 644, porque el servicio
     * de catalogo tiene que poder leerla y no es un secreto.
     */
    private void restrictPermissions(Path path, int permissions) {
        try {
            String mode = (permissions & 0044) != 0 ? "rw-r--r--" : "rw-------";
            Files.setPosixFilePermissions(path,
                    java.nio.file.attribute.PosixFilePermissions.fromString(mode));
        } catch (UnsupportedOperationException | IOException ex) {
            log.warn("No se pudieron ajustar los permisos de {}: {}", path, ex.getMessage());
        }
    }

    /** El directorio de secretos es privado: solo su dueno entra. */
    private void restrictDirectoryPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException | IOException ex) {
            log.warn("No se pudieron ajustar los permisos de {}: {}", path, ex.getMessage());
        }
    }
}
