package com.cinema.user.service;

import java.util.UUID;

public interface AdministrativeUserAccountService {

    void lock(UUID userId);

    void unlock(UUID userId);

    void disable(UUID userId);

    void enable(UUID userId);
}
