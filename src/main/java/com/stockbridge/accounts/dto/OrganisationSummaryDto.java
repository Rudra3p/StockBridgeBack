package com.stockbridge.accounts.dto;

import com.stockbridge.accounts.entity.OrgType;
import com.stockbridge.accounts.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationSummaryDto {
    private UUID id;
    private String legalName;
    private OrgType orgType;
    private String gstNumber;
    private String addressLine;
    private String city;
    private String state;
    private String pinCode;
    private VerificationStatus verificationStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
