package com.stockbridge.listings.dto;

import com.stockbridge.listings.document.ListingCondition;
import com.stockbridge.listings.document.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateListingRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    private String category;

    private ListingCondition condition;

    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0.01", message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    @Min(value = 0, message = "Quantity available cannot be negative")
    private Long quantityAvailable;

    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minimumOrderQuantity;

    private String unit;

    private String locationCity;

    private String locationState;

    private String locationPinCode;

    private ListingStatus status;

    private LocalDate expiryDate;

    private List<String> images;

    private Map<String, Object> attributes;
}
