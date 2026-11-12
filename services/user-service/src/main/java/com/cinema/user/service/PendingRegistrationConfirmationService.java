package com.cinema.user.service;

public interface PendingRegistrationConfirmationService {

    void confirm(String rawToken, String rawPassword);
}
