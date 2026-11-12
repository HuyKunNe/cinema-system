package com.cinema.user.controller;

import com.cinema.user.dto.request.ConfirmEmailVerificationRequest;
import com.cinema.user.dto.request.RegisterUserRequest;
import com.cinema.user.dto.response.RegistrationAcceptedResponse;
import com.cinema.user.service.PendingRegistrationConfirmationService;
import com.cinema.user.service.UserRegistrationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationRegistrationController {

    private final UserRegistrationService userRegistrationService;
    private final PendingRegistrationConfirmationService confirmationService;

    public AuthenticationRegistrationController(
            UserRegistrationService userRegistrationService,
            PendingRegistrationConfirmationService confirmationService) {
        this.userRegistrationService = userRegistrationService;
        this.confirmationService = confirmationService;
    }

    @PostMapping("/registrations")
    public ResponseEntity<RegistrationAcceptedResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(userRegistrationService.register(request));
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<Void> confirmEmail(
            @Valid @RequestBody ConfirmEmailVerificationRequest request) {
        confirmationService.confirm(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }
}
