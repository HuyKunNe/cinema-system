package com.cinema.common.security.jwt;

import java.util.Objects;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class CinemaJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate;

    public CinemaJwtAuthenticationConverter() {
        this(new CinemaJwtGrantedAuthoritiesConverter());
    }

    public CinemaJwtAuthenticationConverter(CinemaJwtGrantedAuthoritiesConverter authoritiesConverter) {

        Objects.requireNonNull(authoritiesConverter, "authoritiesConverter must not be null");

        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setPrincipalClaimName(JwtClaimNames.SUB);
        this.delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");

        return delegate.convert(jwt);
    }
}
