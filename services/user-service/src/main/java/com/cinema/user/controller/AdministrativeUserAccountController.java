package com.cinema.user.controller;

import com.cinema.user.service.AdministrativeUserAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class AdministrativeUserAccountController {

    private final AdministrativeUserAccountService administrativeUserAccountService;

    public AdministrativeUserAccountController(
            AdministrativeUserAccountService administrativeUserAccountService) {

        this.administrativeUserAccountService = administrativeUserAccountService;
    }

    @PatchMapping("/{userId}/lock")
    public ResponseEntity<Void> lockUserAccount(@PathVariable UUID userId) {

        administrativeUserAccountService.lock(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/unlock")
    public ResponseEntity<Void> unlockUserAccount(@PathVariable UUID userId) {

        administrativeUserAccountService.unlock(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Void> disableUserAccount(@PathVariable UUID userId) {

        administrativeUserAccountService.disable(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/enable")
    public ResponseEntity<Void> enableUserAccount(@PathVariable UUID userId) {

        administrativeUserAccountService.enable(userId);

        return ResponseEntity.noContent().build();
    }
}
