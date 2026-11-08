package com.stockbridge.accounts.controller;

import com.stockbridge.accounts.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Role Verification Ping", description = "Endpoints to test role-based access control and method security")
public class PingController {

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin ping test", description = "Accessible ONLY by users with role ADMIN")
    public ResponseEntity<PingResponse> pingAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        PingResponse response = PingResponse.builder()
                .message("Admin ping successful: You have ADMIN privileges.")
                .role("ADMIN")
                .userEmail(userDetails != null ? userDetails.getUsername() : "anonymous")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/ping")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Seller ping test", description = "Accessible ONLY by users with role SELLER")
    public ResponseEntity<PingResponse> pingSeller(@AuthenticationPrincipal UserDetails userDetails) {
        PingResponse response = PingResponse.builder()
                .message("Seller ping successful: You have SELLER privileges.")
                .role("SELLER")
                .userEmail(userDetails != null ? userDetails.getUsername() : "anonymous")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
