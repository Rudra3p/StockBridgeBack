package com.stockbridge.listings.repository;

import com.stockbridge.listings.document.ListingDocument;
import com.stockbridge.listings.document.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingRepository extends MongoRepository<ListingDocument, String> {

    Page<ListingDocument> findBySellerId(UUID sellerId, Pageable pageable);

    Page<ListingDocument> findByStatus(ListingStatus status, Pageable pageable);

    Page<ListingDocument> findBySellerIdAndStatus(UUID sellerId, ListingStatus status, Pageable pageable);

    Optional<ListingDocument> findByIdAndSellerId(String id, UUID sellerId);
}
