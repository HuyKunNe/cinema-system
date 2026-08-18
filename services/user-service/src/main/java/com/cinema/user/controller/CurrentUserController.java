package com.cinema.user.controller;

import com.cinema.user.dto.request.ChangeCurrentUserPasswordRequest;
import com.cinema.user.dto.request.UpdateCurrentUserProfileRequest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.service.UserCredentialService;
import com.cinema.user.service.UserProfileService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class CurrentUserController {

    private final UserProfileService userProfileService;

    private final UserCredentialService userCredentialService;

    public CurrentUserController(
            UserProfileService userProfileService, UserCredentialService userCredentialService) {

        this.userProfileService = userProfileService;
        this.userCredentialService = userCredentialService;
    }

    @GetMapping
    public ResponseEntity<CurrentUserProfileResponse> getCurrentProfile(
            @AuthenticationPrincipal CinemaUserDetails principal) {

        return ResponseEntity.ok(userProfileService.getCurrentProfile(principal.getUserId()));
    }

    @PutMapping
    public ResponseEntity<CurrentUserProfileResponse> updateCurrentProfile(
            @AuthenticationPrincipal CinemaUserDetails principal,
            @Valid @RequestBody UpdateCurrentUserProfileRequest request) {

        return ResponseEntity.ok(
                userProfileService.updateCurrentProfile(principal.getUserId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CinemaUserDetails principal,
            @Valid @RequestBody ChangeCurrentUserPasswordRequest request) {

        userCredentialService.changePassword(
                principal.getUserId(), request.currentPassword(), request.newPassword());

        return ResponseEntity.noContent().build();
    }
}
