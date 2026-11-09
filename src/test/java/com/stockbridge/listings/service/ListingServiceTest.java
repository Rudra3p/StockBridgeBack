package com.stockbridge.listings.service;

import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.Organisation;
import com.stockbridge.accounts.entity.OrgType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private ListingServiceImpl listingService;

    private UserPrincipal sellerPrincipal;
    private UserPrincipal buyerPrincipal;
    private UUID sellerId;
    private UUID orgId;
    private Organisation sellerOrg;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        sellerOrg = Organisation.builder()
                .id(orgId)
                .legalName("Acme Textiles Ltd")
                .orgType(OrgType.MANUFACTURER)
                .gstNumber("24AAACA0000A1Z5")
                .verificationStatus(VerificationStatus.APPROVED)
                .build();

        AppUser sellerUser = AppUser.builder()
                .id(sellerId)
                .email("seller@acme.com")
                .fullName("Acme Seller")
                .role(Role.SELLER)
                .organisation(sellerOrg)
                .enabled(true)
                .build();
        sellerPrincipal = new UserPrincipal(sellerUser);

        AppUser buyerUser = AppUser.builder()
                .id(UUID.randomUUID())
                .email("buyer@retail.com")
                .fullName("Retail Buyer")
                .role(Role.BUYER)
                .enabled(true)
                .build();
        buyerPrincipal = new UserPrincipal(buyerUser);
    }

    @Test
    @DisplayName("Seller creates surplus listing successfully with MongoDB dynamic attributes")
    void createListing_success() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("500 pcs Cotton Shirts Overstock")
                .description("Brand new boxed shirts, leftover from exported batch")
                .category("APPAREL")
                .condition(ListingCondition.BRAND_NEW_OVERSTOCK)
                .originalPrice(BigDecimal.valueOf(1000.00))
                .discountedPrice(BigDecimal.valueOf(450.00))
                .quantityAvailable(500L)
                .minimumOrderQuantity(50)
                .unit("PCS")
                .locationCity("Ahmedabad")
                .locationState("Gujarat")
                .locationPinCode("380001")
                .attributes(Map.of("fabric", "100% Cotton", "gsm", 180, "sizes", "S,M,L,XL"))
                .build();

        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(sellerOrg));
        when(listingRepository.save(any(ListingDocument.class))).thenAnswer(inv -> {
            ListingDocument doc = inv.getArgument(0);
            doc.setId("mongo-list-123");
            return doc;
        });

        ListingResponse response = listingService.createListing(request, sellerPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("mongo-list-123");
        assertThat(response.getTitle()).isEqualTo("500 pcs Cotton Shirts Overstock");
        assertThat(response.getDiscountPercentage()).isEqualTo(55.0);
        assertThat(response.getSellerOrganisationName()).isEqualTo("Acme Textiles Ltd");
        assertThat(response.getAttributes()).containsEntry("fabric", "100% Cotton");
    }

    @Test
    @DisplayName("Buyer cannot create a listing - throws BadRequestException")
    void createListing_buyerRole_throwsBadRequest() {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Unauthorized Listing")
                .description("Test Description")
                .category("APPAREL")
                .condition(ListingCondition.BRAND_NEW_OVERSTOCK)
                .originalPrice(BigDecimal.valueOf(100))
                .discountedPrice(BigDecimal.valueOf(50))
                .quantityAvailable(10L)
                .minimumOrderQuantity(1)
                .build();

        assertThatThrownBy(() -> listingService.createListing(request, buyerPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SELLER or ADMIN accounts can list surplus inventory");
    }

    @Test
    @DisplayName("Get listing by ID returns valid response")
    void getListingById_found() {
        ListingDocument doc = ListingDocument.builder()
                .id("list-abc")
                .sellerId(sellerId)
                .title("Surplus Electronics Cables")
                .category("ELECTRONICS")
                .originalPrice(BigDecimal.valueOf(200))
                .discountedPrice(BigDecimal.valueOf(80))
                .status(ListingStatus.ACTIVE)
                .build();

        when(listingRepository.findById("list-abc")).thenReturn(Optional.of(doc));

        ListingResponse response = listingService.getListingById("list-abc");
        assertThat(response.getId()).isEqualTo("list-abc");
        assertThat(response.getTitle()).isEqualTo("Surplus Electronics Cables");
        assertThat(response.getDiscountPercentage()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Get non-existent listing throws ResourceNotFoundException")
    void getListingById_notFound() {
        when(listingRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.getListingById("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Listing not found with ID: invalid-id");
    }

    @Test
    @DisplayName("Unauthorized user cannot modify someone else's listing")
    void updateListing_unauthorizedUser_throwsBadRequest() {
        ListingDocument doc = ListingDocument.builder()
                .id("list-abc")
                .sellerId(sellerId)
                .title("Surplus Stock")
                .build();

        when(listingRepository.findById("list-abc")).thenReturn(Optional.of(doc));

        UpdateListingRequest updateRequest = UpdateListingRequest.builder()
                .title("Tampered Title")
                .build();

        assertThatThrownBy(() -> listingService.updateListing("list-abc", updateRequest, buyerPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You are not authorized to modify or delete this listing");
    }

    @Test
    @DisplayName("Owner can delete their listing")
    void deleteListing_success() {
        ListingDocument doc = ListingDocument.builder()
                .id("list-abc")
                .sellerId(sellerId)
                .title("Surplus Stock")
                .build();

        when(listingRepository.findById("list-abc")).thenReturn(Optional.of(doc));

        listingService.deleteListing("list-abc", sellerPrincipal);
        verify(listingRepository).delete(doc);
    }
}
