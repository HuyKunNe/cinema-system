package com.cinema.common.security.jwt;

import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import com.cinema.common.security.authentication.AuthenticationUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtTokenProvider {

    private final SecretKey key;

    private final long expiration;

    public JwtTokenProvider(String secret, long expiration) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes());

        this.expiration = expiration;
    }

    public String generateToken(AuthenticationUser user) {

        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("username", user.username())
                .claim("roles", user.roles())
                .claim("permissions", user.permissions())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public JwtClaims parse(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtClaims(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                Set.copyOf(claims.get("roles", Set.class)),
                Set.copyOf(claims.get("permissions", Set.class)));
    }

}
