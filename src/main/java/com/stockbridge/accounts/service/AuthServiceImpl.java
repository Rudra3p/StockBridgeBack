package com.stockbridge.accounts.service;

import java.util.List;
import java.util.UUID;
import com.stockbridge.accounts.dto.*;
import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.Organisation;
import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.entity.VerificationStatus;
import com.stockbridge.accounts.repository.OrganisationRepository;
import com.stockbridge.accounts.repository.UserRepository;
import com.stockbridge.common.exceptions.BadRequestException;
import com.stockbridge.common.exceptions.DuplicateResourceException;
import com.stockbridge.common.exceptions.ResourceNotFoundException;
import com.stockbridge.security.JwtService;
import com.stockbridge.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final com.stockbridge.accounts.repository.PartnerSellerRepository partnerSellerRepository;
    private final com.stockbridge.accounts.repository.WholesaleBuyerRepository wholesaleBuyerRepository;
    private final com.stockbridge.accounts.repository.AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        // Enforce role rule: SELLER or BUYER only; never allow ADMIN
        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Registration for ADMIN role is not permitted.");
        }
        if (request.getRole() != Role.SELLER && request.getRole() != Role.BUYER) {
            throw new BadRequestException("Invalid role. Role must be either SELLER or BUYER.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Check if an account already exists in the DEDICATED table for this specific user type
        if (request.getRole() == Role.BUYER && wholesaleBuyerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' is already registered as a Wholesale Buyer.");
        }
        if (request.getRole() == Role.SELLER && partnerSellerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' is already registered as a Partner Seller.");
        }

        String normalizedGst = request.getGstNumber().trim().toUpperCase();

        // 2. Find existing Organisation with this GST or create new one
        Organisation organisation = organisationRepository.findByGstNumber(normalizedGst)
                .orElseGet(() -> {
                    Organisation newOrg = Organisation.builder()
                            .legalName(request.getLegalName().trim())
                            .orgType(request.getOrgType())
                            .gstNumber(normalizedGst)
                            .addressLine(request.getAddressLine().trim())
                            .city(request.getCity().trim())
                            .state(request.getState().trim())
                            .pinCode(request.getPinCode().trim())
                            .verificationStatus(VerificationStatus.PENDING)
                            .build();
                    return organisationRepository.save(newOrg);
                });

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Persist to dedicated separate table
        UUID savedId;
        if (request.getRole() == Role.SELLER) {
            com.stockbridge.accounts.entity.PartnerSeller partnerSeller = com.stockbridge.accounts.entity.PartnerSeller.builder()
                    .organisation(organisation)
                    .fullName(request.getFullName().trim())
                    .email(normalizedEmail)
                    .phone(request.getPhone().trim())
                    .passwordHash(encodedPassword)
                    .enabled(true)
                    .build();
            savedId = partnerSellerRepository.save(partnerSeller).getId();
        } else {
            com.stockbridge.accounts.entity.WholesaleBuyer wholesaleBuyer = com.stockbridge.accounts.entity.WholesaleBuyer.builder()
                    .organisation(organisation)
                    .fullName(request.getFullName().trim())
                    .email(normalizedEmail)
                    .phone(request.getPhone().trim())
                    .passwordHash(encodedPassword)
                    .enabled(true)
                    .build();
            savedId = wholesaleBuyerRepository.save(wholesaleBuyer).getId();
        }

        // Also synchronize to app_user table for unified queries
        AppUser user = AppUser.builder()
                .id(savedId)
                .organisation(organisation)
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone().trim())
                .passwordHash(encodedPassword)
                .role(request.getRole())
                .enabled(true)
                .build();
        userRepository.save(user);

        log.info("Successfully registered {} '{}' in dedicated table for organisation '{}'",
                request.getRole(), normalizedEmail, organisation.getLegalName());

        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Role targetRole = request.getRole();

        // 1. Authenticate against dedicated table if role is specified
        if (targetRole == Role.BUYER) {
            var buyerOpt = wholesaleBuyerRepository.findByEmailIgnoreCase(normalizedEmail);
            if (buyerOpt.isPresent()) {
                var buyer = buyerOpt.get();
                if (!passwordEncoder.matches(request.getPassword(), buyer.getPasswordHash())) {
                    throw new BadCredentialsException("Invalid email or password");
                }
                UserPrincipal principal = new UserPrincipal(buyer);
                String token = jwtService.generateToken(principal);
                return AuthResponse.builder()
                        .token(token)
                        .user(UserInfoResponse.builder()
                                .id(buyer.getId())
                                .name(buyer.getFullName())
                                .email(buyer.getEmail())
                                .role(Role.BUYER)
                                .organisationId(buyer.getOrganisation().getId())
                                .verificationStatus(buyer.getOrganisation().getVerificationStatus())
                                .build())
                        .build();
            }
        } else if (targetRole == Role.SELLER) {
            var sellerOpt = partnerSellerRepository.findByEmailIgnoreCase(normalizedEmail);
            if (sellerOpt.isPresent()) {
                var seller = sellerOpt.get();
                if (!passwordEncoder.matches(request.getPassword(), seller.getPasswordHash())) {
                    throw new BadCredentialsException("Invalid email or password");
                }
                UserPrincipal principal = new UserPrincipal(seller);
                String token = jwtService.generateToken(principal);
                return AuthResponse.builder()
                        .token(token)
                        .user(UserInfoResponse.builder()
                                .id(seller.getId())
                                .name(seller.getFullName())
                                .email(seller.getEmail())
                                .role(Role.SELLER)
                                .organisationId(seller.getOrganisation().getId())
                                .verificationStatus(seller.getOrganisation().getVerificationStatus())
                                .build())
                        .build();
            }
        } else if (targetRole == Role.ADMIN) {
            var adminOpt = adminUserRepository.findByEmailIgnoreCase(normalizedEmail);
            if (adminOpt.isPresent()) {
                var admin = adminOpt.get();
                if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
                    throw new BadCredentialsException("Invalid email or password");
                }
                UserPrincipal principal = new UserPrincipal(admin);
                String token = jwtService.generateToken(principal);
                return AuthResponse.builder()
                        .token(token)
                        .user(UserInfoResponse.builder()
                                .id(admin.getId())
                                .name(admin.getFullName())
                                .email(admin.getEmail())
                                .role(Role.ADMIN)
                                .organisationId(null)
                                .verificationStatus(VerificationStatus.APPROVED)
                                .build())
                        .build();
            }
        }

        // Fallback to app_user repository
        AppUser user;
        if (targetRole != null) {
            user = userRepository.findByEmailAndRole(normalizedEmail, targetRole)
                    .orElseThrow(() -> new BadCredentialsException("No " + targetRole + " account found with email: " + normalizedEmail));
        } else {
            List<AppUser> users = userRepository.findAllByEmail(normalizedEmail);
            if (users.isEmpty()) {
                throw new BadCredentialsException("Invalid email or password");
            }
            user = users.stream()
                    .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                    .findFirst()
                    .orElse(users.get(0));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String token = jwtService.generateToken(userPrincipal);

        return AuthResponse.builder()
                .token(token)
                .user(UserInfoResponse.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .organisationId(user.getOrganisation() != null ? user.getOrganisation().getId() : null)
                        .verificationStatus(user.getOrganisation() != null ? user.getOrganisation().getVerificationStatus() : VerificationStatus.APPROVED)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        List<AppUser> users = userRepository.findAllByEmail(normalizedEmail);
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return mapToUserProfileResponse(users.get(0));
    }

    private UserProfileResponse mapToUserProfileResponse(AppUser user) {
        Organisation org = user.getOrganisation();
        OrganisationSummaryDto orgSummary = org != null ? OrganisationSummaryDto.builder()
                .id(org.getId())
                .legalName(org.getLegalName())
                .orgType(org.getOrgType())
                .gstNumber(org.getGstNumber())
                .addressLine(org.getAddressLine())
                .city(org.getCity())
                .state(org.getState())
                .pinCode(org.getPinCode())
                .verificationStatus(org.getVerificationStatus())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build() : null;

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .organisation(orgSummary)
                .build();
    }
}
