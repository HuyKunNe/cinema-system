package com.cinema.user.oauth2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

record TestRsaKeyMaterial(
        String privateKeyLocation,
        String publicKeyLocation) {

    static TestRsaKeyMaterial generate() {
        try {
            Path directory = Files.createTempDirectory(
                    "cinema-jwt-test-");

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            KeyPair keyPair = generator.generateKeyPair();

            Path privateKeyPath = directory.resolve("private.pem");
            Path publicKeyPath = directory.resolve("public.pem");

            writePem(
                    privateKeyPath,
                    "PRIVATE KEY",
                    keyPair.getPrivate().getEncoded());

            writePem(
                    publicKeyPath,
                    "PUBLIC KEY",
                    keyPair.getPublic().getEncoded());

            privateKeyPath.toFile().deleteOnExit();
            publicKeyPath.toFile().deleteOnExit();
            directory.toFile().deleteOnExit();

            return new TestRsaKeyMaterial(
                    privateKeyPath.toUri().toString(),
                    publicKeyPath.toUri().toString());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void writePem(
            Path path,
            String type,
            byte[] encoded) throws IOException {

        String body = Base64
                .getMimeEncoder(64, new byte[] { '\n' })
                .encodeToString(encoded);

        String pem = "-----BEGIN " + type + "-----\n"
                + body
                + "\n-----END " + type + "-----\n";

        Files.writeString(
                path,
                pem,
                StandardCharsets.US_ASCII);
    }
}
