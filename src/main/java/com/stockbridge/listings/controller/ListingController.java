package com.stockbridge.listings.controller;

import com.stockbridge.listings.document.ListingCondition;
import com.stockbridge.listings.document.ListingStatus;
import com.stockbridge.listings.dto.CreateListingRequest;
import com.stockbridge.listings.dto.ListingResponse;
import com.stockbridge.listings.dto.UpdateListingRequest;
import com.stockbridge.listings.service.ListingService;
import com.stockbridge.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Tag(name = "Surplus Listings", description = "Endpoints for managing and browsing B2B surplus product listings")
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Create surplus listing", description = "Allows verified sellers to post excess inventory", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ListingResponse response = listingService.createListing(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Search & browse listings", description = "Public / Buyer marketplace catalog search with filters and pagination")
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ListingCondition condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ListingResponse> results = listingService.searchListings(search, category, condition, minPrice, maxPrice, status, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Get my listings", description = "Returns all listings published by the authenticated seller", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<ListingResponse>> getMyListings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ListingResponse> results = listingService.getMyListings(currentUser, status, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing details", description = "Returns complete specification and details for a surplus listing")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable String id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Update listing", description = "Updates details, price, quantity, or status of an existing listing", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable String id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ListingResponse response = listingService.updateListing(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Delete listing", description = "Deletes a surplus listing from MongoDB", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> deleteListing(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        listingService.deleteListing(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully", "id", id));
    }
}
