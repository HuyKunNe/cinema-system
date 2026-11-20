package com.cinema.user.controller;

import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.user.service.StaffAccessService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/staff-context")
public class StaffAccessController {

    private final StaffAccessService service;

    public StaffAccessController(StaffAccessService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<StaffAccessService.Context> getStaffAccessContext() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.current(CurrentUser.id()));
    }
}
