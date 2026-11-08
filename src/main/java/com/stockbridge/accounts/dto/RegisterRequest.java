package com.stockbridge.accounts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockbridge.accounts.entity.OrgType;
import com.stockbridge.accounts.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // Organisation details
    @NotBlank(message = "Organisation legal name is required")
    @Schema(example = "Acme Surplus Wholesale Pvt Ltd")
    private String legalName;

    @NotNull(message = "Organisation type is required")
    @Schema(example = "WHOLESALER", description = "MANUFACTURER, DISTRIBUTOR, WHOLESALER, RETAILER, ONLINE_RESELLER, OTHER")
    private OrgType orgType;

    @NotBlank(message = "GST number is required")
    @Schema(example = "29ABCDE1234F1Z5")
    private String gstNumber;

    @NotBlank(message = "Address line is required")
    @Schema(example = "Plot 42, Industrial Area, Phase 1")
    private String addressLine;

    @NotBlank(message = "City is required")
    @Schema(example = "Bengaluru")
    private String city;

    @NotBlank(message = "State is required")
    @Schema(example = "Karnataka")
    private String state;

    @NotBlank(message = "PIN code is required")
    @Schema(example = "560058")
    private String pinCode;

    // User details
    @NotBlank(message = "Full name is required")
    @Schema(example = "Rahul Sharma")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "rahul@acmewholesale.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(example = "+919876543210")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(example = "SecurePass123!", minLength = 8)
    private String password;

    @NotNull(message = "Role is required (SELLER or BUYER)")
    @Schema(example = "SELLER", description = "Role must be SELLER or BUYER only. ADMIN is not allowed.")
    private Role role;

    // Support nested payload format: { organisation: {...}, user: {...} }
    @JsonProperty("organisation")
    public void setOrganisation(OrganisationNestedDto org) {
        if (org != null) {
            if (org.getLegalName() != null) this.legalName = org.getLegalName();
            if (org.getOrgType() != null) this.orgType = org.getOrgType();
            if (org.getGstNumber() != null) this.gstNumber = org.getGstNumber();
            if (org.getAddressLine() != null) this.addressLine = org.getAddressLine();
            if (org.getCity() != null) this.city = org.getCity();
            if (org.getState() != null) this.state = org.getState();
            if (org.getPinCode() != null) this.pinCode = org.getPinCode();
        }
    }

    @JsonProperty("user")
    public void setUser(UserNestedDto u) {
        if (u != null) {
            if (u.getFullName() != null) this.fullName = u.getFullName();
            if (u.getEmail() != null) this.email = u.getEmail();
            if (u.getPhone() != null) this.phone = u.getPhone();
            if (u.getPassword() != null) this.password = u.getPassword();
            if (u.getRole() != null) this.role = u.getRole();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganisationNestedDto {
        private String legalName;
        private OrgType orgType;
        private String gstNumber;
        private String addressLine;
        private String city;
        private String state;
        private String pinCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserNestedDto {
        private String fullName;
        private String email;
        private String phone;
        private String password;
        private Role role;
    }
}
