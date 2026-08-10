package com.cinema.user.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public record RsaSigningKeyPair(
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey) {
}
