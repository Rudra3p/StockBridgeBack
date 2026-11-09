package com.stockbridge.listings.service;

import com.stockbridge.listings.document.ListingCondition;
import com.stockbridge.listings.document.ListingStatus;
import com.stockbridge.listings.dto.CreateListingRequest;
import com.stockbridge.listings.dto.ListingResponse;
import com.stockbridge.listings.dto.UpdateListingRequest;
import com.stockbridge.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {

    ListingResponse createListing(CreateListingRequest request, UserPrincipal currentUser);

    ListingResponse getListingById(String id);

    Page<ListingResponse> searchListings(
            String search,
            String category,
            ListingCondition condition,
            Double minPrice,
            Double maxPrice,
            ListingStatus status,
            Pageable pageable
    );

    Page<ListingResponse> getMyListings(UserPrincipal currentUser, ListingStatus status, Pageable pageable);

    ListingResponse updateListing(String id, UpdateListingRequest request, UserPrincipal currentUser);

    void deleteListing(String id, UserPrincipal currentUser);
}
