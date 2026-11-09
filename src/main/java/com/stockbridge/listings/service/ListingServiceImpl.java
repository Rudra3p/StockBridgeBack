package com.stockbridge.listings.service;

import com.stockbridge.accounts.entity.Organisation;
import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.entity.VerificationStatus;
import com.stockbridge.accounts.repository.OrganisationRepository;
import com.stockbridge.common.exceptions.BadRequestException;
import com.stockbridge.common.exceptions.ResourceNotFoundException;
import com.stockbridge.listings.document.ListingCondition;
import com.stockbridge.listings.document.ListingDocument;
import com.stockbridge.listings.document.ListingStatus;
import com.stockbridge.listings.dto.CreateListingRequest;
import com.stockbridge.listings.dto.ListingResponse;
import com.stockbridge.listings.dto.UpdateListingRequest;
import com.stockbridge.listings.repository.ListingRepository;
import com.stockbridge.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final MongoTemplate mongoTemplate;
    private final OrganisationRepository organisationRepository;

    @Override
    public ListingResponse createListing(CreateListingRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.SELLER && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only SELLER or ADMIN accounts can list surplus inventory.");
        }

        String orgName = "Unknown Business";
        if (currentUser.getOrganisationId() != null) {
            Organisation org = organisationRepository.findById(currentUser.getOrganisationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organisation not found for current user."));

            if (org.getVerificationStatus() == VerificationStatus.REJECTED) {
                throw new BadRequestException("Cannot create listings: Your business registration has been rejected.");
            }
            orgName = org.getLegalName();
        }

        ListingDocument doc = ListingDocument.builder()
                .sellerId(currentUser.getId())
                .sellerEmail(currentUser.getUsername())
                .sellerOrganisationId(currentUser.getOrganisationId())
                .sellerOrganisationName(orgName)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory().trim().toUpperCase())
                .condition(request.getCondition())
                .originalPrice(request.getOriginalPrice())
                .discountedPrice(request.getDiscountedPrice())
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency().toUpperCase() : "INR")
                .quantityAvailable(request.getQuantityAvailable())
                .minimumOrderQuantity(request.getMinimumOrderQuantity() != null ? request.getMinimumOrderQuantity() : 1)
                .unit(request.getUnit() != null && !request.getUnit().isBlank() ? request.getUnit().toUpperCase() : "PCS")
                .locationCity(request.getLocationCity())
                .locationState(request.getLocationState())
                .locationPinCode(request.getLocationPinCode())
                .status(ListingStatus.ACTIVE)
                .expiryDate(request.getExpiryDate())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ListingDocument saved = listingRepository.save(doc);
        log.info("Surplus listing created successfully with ID: {} by seller: {}", saved.getId(), currentUser.getUsername());
        return ListingResponse.fromDocument(saved);
    }

    @Override
    public ListingResponse getListingById(String id) {
        ListingDocument doc = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));
        return ListingResponse.fromDocument(doc);
    }

    @Override
    public Page<ListingResponse> searchListings(
            String search,
            String category,
            ListingCondition condition,
            Double minPrice,
            Double maxPrice,
            ListingStatus status,
            Pageable pageable
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        ListingStatus targetStatus = status != null ? status : ListingStatus.ACTIVE;
        criteriaList.add(Criteria.where("status").is(targetStatus));

        if (category != null && !category.isBlank()) {
            criteriaList.add(Criteria.where("category").is(category.trim().toUpperCase()));
        }

        if (condition != null) {
            criteriaList.add(Criteria.where("condition").is(condition));
        }

        if (minPrice != null && maxPrice != null) {
            criteriaList.add(Criteria.where("discountedPrice")
                    .gte(BigDecimal.valueOf(minPrice))
                    .lte(BigDecimal.valueOf(maxPrice)));
        } else if (minPrice != null) {
            criteriaList.add(Criteria.where("discountedPrice").gte(BigDecimal.valueOf(minPrice)));
        } else if (maxPrice != null) {
            criteriaList.add(Criteria.where("discountedPrice").lte(BigDecimal.valueOf(maxPrice)));
        }

        if (search != null && !search.isBlank()) {
            Pattern regex = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex),
                    Criteria.where("description").regex(regex),
                    Criteria.where("sellerOrganisationName").regex(regex)
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, ListingDocument.class);
        query.with(pageable);
        List<ListingDocument> documents = mongoTemplate.find(query, ListingDocument.class);

        List<ListingResponse> responses = documents.stream()
                .map(ListingResponse::fromDocument)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    public Page<ListingResponse> getMyListings(UserPrincipal currentUser, ListingStatus status, Pageable pageable) {
        Page<ListingDocument> page;
        if (status != null) {
            page = listingRepository.findBySellerIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            page = listingRepository.findBySellerId(currentUser.getId(), pageable);
        }
        return page.map(ListingResponse::fromDocument);
    }

    @Override
    public ListingResponse updateListing(String id, UpdateListingRequest request, UserPrincipal currentUser) {
        ListingDocument doc = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        verifyOwnership(doc, currentUser);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            doc.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            doc.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            doc.setCategory(request.getCategory().trim().toUpperCase());
        }
        if (request.getCondition() != null) {
            doc.setCondition(request.getCondition());
        }
        if (request.getOriginalPrice() != null) {
            doc.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getDiscountedPrice() != null) {
            doc.setDiscountedPrice(request.getDiscountedPrice());
        }
        if (request.getQuantityAvailable() != null) {
            doc.setQuantityAvailable(request.getQuantityAvailable());
        }
        if (request.getMinimumOrderQuantity() != null) {
            doc.setMinimumOrderQuantity(request.getMinimumOrderQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            doc.setUnit(request.getUnit().toUpperCase());
        }
        if (request.getLocationCity() != null) {
            doc.setLocationCity(request.getLocationCity());
        }
        if (request.getLocationState() != null) {
            doc.setLocationState(request.getLocationState());
        }
        if (request.getLocationPinCode() != null) {
            doc.setLocationPinCode(request.getLocationPinCode());
        }
        if (request.getStatus() != null) {
            doc.setStatus(request.getStatus());
        }
        if (request.getExpiryDate() != null) {
            doc.setExpiryDate(request.getExpiryDate());
        }
        if (request.getImages() != null) {
            doc.setImages(request.getImages());
        }
        if (request.getAttributes() != null) {
            if (doc.getAttributes() == null) {
                doc.setAttributes(new HashMap<>());
            }
            doc.getAttributes().putAll(request.getAttributes());
        }

        doc.setUpdatedAt(Instant.now());
        ListingDocument updated = listingRepository.save(doc);
        log.info("Listing {} updated by user {}", id, currentUser.getUsername());
        return ListingResponse.fromDocument(updated);
    }

    @Override
    public void deleteListing(String id, UserPrincipal currentUser) {
        ListingDocument doc = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        verifyOwnership(doc, currentUser);
        listingRepository.delete(doc);
        log.info("Listing {} deleted by user {}", id, currentUser.getUsername());
    }

    private void verifyOwnership(ListingDocument doc, UserPrincipal currentUser) {
        if (!doc.getSellerId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new BadRequestException("You are not authorized to modify or delete this listing.");
        }
    }
}
