package com.cinema.user.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import com.cinema.user.config.JwtSigningKeyProperties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtSigningKeyLoaderTest {

    private static final String KEY_ID = "cinema-user-test-2026-01";

    @TempDir
    Path temporaryDirectory;

    private JwtSigningKeyLoader keyLoader;
    private KeyPair firstKeyPair;
    private KeyPair secondKeyPair;

    @BeforeAll
    void setUp() throws NoSuchAlgorithmException {
        keyLoader = new JwtSigningKeyLoader(new DefaultResourceLoader());
        firstKeyPair = generateRsaKeyPair(2048);
        secondKeyPair = generateRsaKeyPair(2048);
    }

    @Test
    void shouldLoadMatchingRsaKeyPair() throws IOException {
        Path privateKey = writePrivateKey(
                "matching-private.pem",
                firstKeyPair.getPrivate());
        Path publicKey = writePublicKey(
                "matching-public.pem",
                firstKeyPair.getPublic());

        RsaSigningKeyPair result = keyLoader.load(
                properties(privateKey, publicKey));

        assertThat(result.privateKey().getModulus())
                .isEqualTo(((RSAPrivateKey) firstKeyPair.getPrivate()).getModulus());
        assertThat(result.publicKey().getModulus())
                .isEqualTo(((RSAPublicKey) firstKeyPair.getPublic()).getModulus());
        assertThat(result.publicKey().getModulus().bitLength())
                .isGreaterThanOrEqualTo(2048);
    }

    @Test
    void shouldRejectUnreadablePrivateKey() throws IOException {
        Path missingPrivateKey = temporaryDirectory.resolve("missing-private.pem");
        Path publicKey = writePublicKey(
                "readable-public.pem",
                firstKeyPair.getPublic());

        assertThatThrownBy(() -> keyLoader.load(
                properties(missingPrivateKey, publicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT private signing key is not readable");
    }

    @Test
    void shouldRejectUnreadablePublicKey() throws IOException {
        Path privateKey = writePrivateKey(
                "readable-private.pem",
                firstKeyPair.getPrivate());
        Path missingPublicKey = temporaryDirectory.resolve("missing-public.pem");

        assertThatThrownBy(() -> keyLoader.load(
                properties(privateKey, missingPublicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT public signing key is not readable");
    }

    @Test
    void shouldRejectMalformedPrivateKey() throws IOException {
        Path privateKey = writeText("malformed-private.pem", "not-a-private-key");
        Path publicKey = writePublicKey(
                "valid-public.pem",
                firstKeyPair.getPublic());

        assertThatThrownBy(() -> keyLoader.load(
                properties(privateKey, publicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to load JWT private signing key")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectMalformedPublicKey() throws IOException {
        Path privateKey = writePrivateKey(
                "valid-private.pem",
                firstKeyPair.getPrivate());
        Path publicKey = writeText("malformed-public.pem", "not-a-public-key");

        assertThatThrownBy(() -> keyLoader.load(
                properties(privateKey, publicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to load JWT public signing key")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectMismatchedKeyPair() throws IOException {
        Path privateKey = writePrivateKey(
                "first-private.pem",
                firstKeyPair.getPrivate());
        Path publicKey = writePublicKey(
                "second-public.pem",
                secondKeyPair.getPublic());

        assertThatThrownBy(() -> keyLoader.load(
                properties(privateKey, publicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT public and private signing keys do not form a key pair");
    }

    @Test
    void shouldRejectRsaKeySmallerThan2048Bits()
            throws NoSuchAlgorithmException, IOException {

        KeyPair weakKeyPair = generateRsaKeyPair(1024);
        Path privateKey = writePrivateKey(
                "weak-private.pem",
                weakKeyPair.getPrivate());
        Path publicKey = writePublicKey(
                "weak-public.pem",
                weakKeyPair.getPublic());

        assertThatThrownBy(() -> keyLoader.load(
                properties(privateKey, publicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT RSA signing key must contain at least 2048 bits");
    }

    private JwtSigningKeyProperties properties(
            Path privateKey,
            Path publicKey) {

        return new JwtSigningKeyProperties(
                true,
                KEY_ID,
                privateKey.toUri().toString(),
                publicKey.toUri().toString());
    }

    private Path writePrivateKey(String filename, PrivateKey privateKey)
            throws IOException {

        return writePem(filename, "PRIVATE KEY", privateKey.getEncoded());
    }

    private Path writePublicKey(String filename, PublicKey publicKey)
            throws IOException {

        return writePem(filename, "PUBLIC KEY", publicKey.getEncoded());
    }

    private Path writePem(String filename, String type, byte[] encoded)
            throws IOException {

        String body = Base64.getMimeEncoder(64, new byte[] { '\n' })
                .encodeToString(encoded);

        return writeText(
                filename,
                "-----BEGIN " + type + "-----\n"
                        + body
                        + "\n-----END " + type + "-----\n");
    }

    private Path writeText(String filename, String content) throws IOException {
        Path path = temporaryDirectory.resolve(filename);
        Files.writeString(path, content, StandardCharsets.US_ASCII);
        return path;
    }

    private KeyPair generateRsaKeyPair(int keySize)
            throws NoSuchAlgorithmException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }
}
