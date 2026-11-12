package com.cinema.user.service;

import com.cinema.user.dto.request.RegisterUserRequest;
import com.cinema.user.dto.response.RegistrationAcceptedResponse;

public interface UserRegistrationService {

    RegistrationAcceptedResponse register(RegisterUserRequest request);
}
