package com.stockbridge.listings.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "surplus_listings")
@CompoundIndexes({
        @CompoundIndex(name = "status_category_idx", def = "{'status': 1, 'category': 1}"),
        @CompoundIndex(name = "status_discountedPrice_idx", def = "{'status': 1, 'discountedPrice': 1}"),
        @CompoundIndex(name = "seller_status_idx", def = "{'sellerId': 1, 'status': 1}")
})
public class ListingDocument {

    @Id
    private String id;

    // Relational references to PostgreSQL entities
    @Indexed
    private UUID sellerId;

    private String sellerEmail;

    @Indexed
    private UUID sellerOrganisationId;

    private String sellerOrganisationName;

    @Indexed
    private String title;

    private String description;

    @Indexed
    private String category;

    private ListingCondition condition;

    private BigDecimal originalPrice;

    private BigDecimal discountedPrice;

    @Builder.Default
    private String currency = "INR";

    private Long quantityAvailable;

    @Builder.Default
    private Integer minimumOrderQuantity = 1;

    @Builder.Default
    private String unit = "PCS";

    private String locationCity;

    private String locationState;

    private String locationPinCode;

    @Indexed
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    private LocalDate expiryDate;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    // Dynamic schema for flexible B2B product specs (e.g., brand, batchNo, shelfLifeMonths)
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
