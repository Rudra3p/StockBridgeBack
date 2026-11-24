package com.stockbridge.accounts.controller;

import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.Organisation;
import com.stockbridge.accounts.entity.VerificationStatus;
import com.stockbridge.accounts.repository.OrganisationRepository;
import com.stockbridge.accounts.repository.UserRepository;
import com.stockbridge.listings.document.ListingDocument;
import com.stockbridge.listings.document.ListingStatus;
import com.stockbridge.listings.repository.ListingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Endpoints for platform administration, KYC approvals, and auditing")
public class AdminController {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    /**
     * Platform health & connectivity check for Admin Console
     */
    @GetMapping("/health")
    @Operation(summary = "Check backend and database health for admin console")
    public ResponseEntity<Map<String, Object>> getAdminHealth() {
        long orgCount = organisationRepository.count();
        long userCount = userRepository.count();
        long listingCount = 0;
        try {
            listingCount = listingRepository.count();
        } catch (Exception e) {
            log.warn("MongoDB listing count check failed: {}", e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("database", "CONNECTED");
        response.put("postgres", "UP");
        response.put("totalOrganisations", orgCount);
        response.put("totalUsers", userCount);
        response.put("totalListings", listingCount);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all organisations for KYC verification
     */
    @GetMapping("/organisations")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all registered organisations")
    public ResponseEntity<List<Map<String, Object>>> getOrganisations() {
        List<Organisation> orgs = organisationRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Organisation org : orgs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", org.getId().toString());
            map.put("legalName", org.getLegalName());
            map.put("orgType", org.getOrgType().name());
            map.put("gstNumber", org.getGstNumber());
            map.put("city", org.getCity());
            map.put("state", org.getState());
            map.put("addressLine", org.getAddressLine());
            map.put("pinCode", org.getPinCode());
            map.put("verificationStatus", org.getVerificationStatus().name());
            map.put("registeredAt", org.getCreatedAt() != null ? org.getCreatedAt().toString() : Instant.now().toString());
            results.add(map);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Update KYC Verification Status of an organisation
     */
    @PutMapping("/organisations/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update organisation verification status")
    public ResponseEntity<Map<String, Object>> updateOrganisationStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }

        VerificationStatus status;
        try {
            status = VerificationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid verification status: " + statusStr));
        }

        Optional<Organisation> optOrg = organisationRepository.findById(id);
        if (optOrg.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Organisation org = optOrg.get();
        org.setVerificationStatus(status);
        Organisation updated = organisationRepository.save(org);

        log.info("Admin updated organisation '{}' ({}) verification status to '{}'",
                updated.getLegalName(), updated.getId(), status);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", updated.getId().toString());
        result.put("verificationStatus", updated.getVerificationStatus().name());
        result.put("message", "Organisation verification status updated to " + status);

        return ResponseEntity.ok(result);
    }

    /**
     * Get all users for User Account Controls
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all platform user accounts")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<AppUser> users = userRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (AppUser u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId().toString());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRole().name());
            map.put("enabled", u.isEnabled());
            map.put("orgName", u.getOrganisation() != null ? u.getOrganisation().getLegalName() : "N/A");
            map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : Instant.now().toString());
            results.add(map);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Toggle enabled status of a user
     */
    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle user enabled status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Boolean> body
    ) {
        Optional<AppUser> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = optUser.get();
        if (body != null && body.containsKey("enabled")) {
            user.setEnabled(body.get("enabled"));
        } else {
            user.setEnabled(!user.isEnabled());
        }

        AppUser saved = userRepository.save(user);
        log.info("Admin toggled user '{}' ({}) enabled status to {}", saved.getEmail(), saved.getId(), saved.isEnabled());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", saved.getId().toString(),
                "enabled", saved.isEnabled(),
                "message", "User account status updated"
        ));
    }

    /**
     * Get all surplus listings for QA audit
     */
    @GetMapping("/listings")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all listings for QA review")
    public ResponseEntity<List<ListingDocument>> getListingsForQA() {
        List<ListingDocument> listings = listingRepository.findAll();
        return ResponseEntity.ok(listings);
    }

    /**
     * Update Listing QA Status
     */
    @PutMapping("/listings/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update listing QA audit status")
    public ResponseEntity<Map<String, Object>> updateListingStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }

        ListingStatus status;
        try {
            status = ListingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid listing status: " + statusStr));
        }

        Optional<ListingDocument> optListing = listingRepository.findById(id);
        if (optListing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ListingDocument doc = optListing.get();
        doc.setStatus(status);
        ListingDocument saved = listingRepository.save(doc);

        log.info("Admin updated listing '{}' ({}) QA status to '{}'", saved.getTitle(), saved.getId(), status);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", saved.getId(),
                "status", saved.getStatus().name(),
                "message", "Listing QA status updated to " + status
        ));
    }
}
