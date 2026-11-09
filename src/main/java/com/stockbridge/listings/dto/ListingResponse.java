package com.stockbridge.listings.dto;

import com.stockbridge.listings.document.ListingCondition;
import com.stockbridge.listings.document.ListingDocument;
import com.stockbridge.listings.document.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {

    private String id;
    private UUID sellerId;
    private String sellerEmail;
    private UUID sellerOrganisationId;
    private String sellerOrganisationName;

    private String title;
    private String description;
    private String category;
    private ListingCondition condition;

    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Double discountPercentage;
    private String currency;

    private Long quantityAvailable;
    private Integer minimumOrderQuantity;
    private String unit;

    private String locationCity;
    private String locationState;
    private String locationPinCode;

    private ListingStatus status;
    private LocalDate expiryDate;
    private List<String> images;
    private Map<String, Object> attributes;

    private Instant createdAt;
    private Instant updatedAt;

    public static ListingResponse fromDocument(ListingDocument doc) {
        Double discount = null;
        if (doc.getOriginalPrice() != null && doc.getDiscountedPrice() != null
                && doc.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = doc.getOriginalPrice().subtract(doc.getDiscountedPrice());
            BigDecimal pct = diff.divide(doc.getOriginalPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            discount = pct.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return ListingResponse.builder()
                .id(doc.getId())
                .sellerId(doc.getSellerId())
                .sellerEmail(doc.getSellerEmail())
                .sellerOrganisationId(doc.getSellerOrganisationId())
                .sellerOrganisationName(doc.getSellerOrganisationName())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .condition(doc.getCondition())
                .originalPrice(doc.getOriginalPrice())
                .discountedPrice(doc.getDiscountedPrice())
                .discountPercentage(discount)
                .currency(doc.getCurrency())
                .quantityAvailable(doc.getQuantityAvailable())
                .minimumOrderQuantity(doc.getMinimumOrderQuantity())
                .unit(doc.getUnit())
                .locationCity(doc.getLocationCity())
                .locationState(doc.getLocationState())
                .locationPinCode(doc.getLocationPinCode())
                .status(doc.getStatus())
                .expiryDate(doc.getExpiryDate())
                .images(doc.getImages())
                .attributes(doc.getAttributes())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
