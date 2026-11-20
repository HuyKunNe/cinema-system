package com.cinema.user.controller;

import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;
import com.cinema.user.dto.request.AssignStaffCinemaRequest;
import com.cinema.user.dto.request.CreateStaffAccountRequest;
import com.cinema.user.dto.response.StaffAccountResponse;
import com.cinema.user.service.AdministrativeStaffService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/users/staff")
public class AdministrativeStaffController {

    private final AdministrativeStaffService service;

    public AdministrativeStaffController(AdministrativeStaffService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StaffAccountResponse> createStaffAccount(
            @Valid @RequestBody CreateStaffAccountRequest request) {
        var response = service.create(request, CurrentUser.id());
        return ResponseEntity.created(URI.create("/api/v1/users/staff/" + response.userId()))
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<StaffAccountResponse>> listStaffAccounts(
            @RequestParam(required = false) @UuidV7 UUID cinemaId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(PageResponseMapper.map(service.list(cinemaId, page, size)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<StaffAccountResponse> getStaffAccount(@PathVariable @UuidV7 UUID userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(userId));
    }

    @PutMapping("/{userId}/cinema")
    public ResponseEntity<StaffAccountResponse> assignStaffCinema(
            @PathVariable @UuidV7 UUID userId,
            @Valid @RequestBody AssignStaffCinemaRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.reassign(userId, request.cinemaId(), CurrentUser.id()));
    }
}
