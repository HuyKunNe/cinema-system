package com.cinema.user.oauth2.token;

public interface RefreshTokenHasher {

    String hash(String rawToken);
}
