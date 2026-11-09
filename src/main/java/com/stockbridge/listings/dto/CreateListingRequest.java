package com.stockbridge.listings.dto;

import com.stockbridge.listings.document.ListingCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateListingRequest {

    @NotBlank(message = "Listing title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Listing condition is required")
    private ListingCondition condition;

    @NotNull(message = "Original price is required")
    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    private BigDecimal originalPrice;

    @NotNull(message = "Discounted surplus price is required")
    @DecimalMin(value = "0.01", message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    private String currency;

    @NotNull(message = "Quantity available is required")
    @Min(value = 1, message = "Quantity available must be at least 1")
    private Long quantityAvailable;

    @NotNull(message = "Minimum order quantity is required")
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minimumOrderQuantity;

    private String unit;

    private String locationCity;

    private String locationState;

    private String locationPinCode;

    private LocalDate expiryDate;

    private List<String> images;

    // Flexible custom attributes (e.g. brand, color, dimensions, warranty, batchNumber)
    private Map<String, Object> attributes;
}
