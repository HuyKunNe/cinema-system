package com.cinema.user.controller;

import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.service.UserProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class CurrentUserController {

    private final UserProfileService userProfileService;

    public CurrentUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<CurrentUserProfileResponse> getCurrentProfile(
            @AuthenticationPrincipal CinemaUserDetails principal) {

        return ResponseEntity.ok(userProfileService.getCurrentProfile(principal.getUserId()));
    }
}
