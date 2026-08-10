package com.cinema.user.security.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;

import com.cinema.user.config.JwtSigningKeyProperties;

@Component
public class JwtSigningKeyLoader {

    private static final int MINIMUM_RSA_KEY_SIZE = 2048;

    private final ResourceLoader resourceLoader;

    public JwtSigningKeyLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public RsaSigningKeyPair load(JwtSigningKeyProperties properties) {
        RSAPublicKey publicKey = loadPublicKey(properties.publicKeyLocation());
        RSAPrivateKey privateKey = loadPrivateKey(properties.privateKeyLocation());

        validateKeyPair(publicKey, privateKey);

        return new RsaSigningKeyPair(publicKey, privateKey);
    }

    private RSAPublicKey loadPublicKey(String location) {
        Resource resource = resourceLoader.getResource(location);

        requireReadable(resource, "public");

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw signingKeyFailure("Unable to load JWT public signing key", exception);
        }
    }

    private RSAPrivateKey loadPrivateKey(String location) {
        Resource resource = resourceLoader.getResource(location);

        requireReadable(resource, "private");

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw signingKeyFailure("Unable to load JWT private signing key", exception);
        }
    }

    private void requireReadable(Resource resource, String keyType) {
        if (!resource.exists() || !resource.isReadable()) {
            throw signingKeyFailure(
                    "JWT " + keyType + " signing key is not readable",
                    null);
        }
    }

    private void validateKeyPair(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey) {

        if (publicKey.getModulus().bitLength() < MINIMUM_RSA_KEY_SIZE) {
            throw signingKeyFailure(
                    "JWT RSA signing key must contain at least 2048 bits",
                    null);
        }

        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw signingKeyFailure(
                    "JWT public and private signing keys do not form a key pair",
                    null);
        }
    }

    private IllegalStateException signingKeyFailure(
            String message,
            Throwable cause) {

        return cause == null
                ? new IllegalStateException(message)
                : new IllegalStateException(message, cause);
    }
}
