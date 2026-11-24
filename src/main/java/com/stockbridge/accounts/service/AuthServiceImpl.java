package com.stockbridge.accounts.service;

import java.util.List;
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

        // Check if an account already exists for THIS specific role
        if (userRepository.existsByEmailAndRole(normalizedEmail, request.getRole())) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' is already registered as " + request.getRole() + ".");
        }

        String normalizedGst = request.getGstNumber().trim().toUpperCase();

        // 1. Find existing Organisation with this GST or create new one
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

        // 2. Persist to dedicated role table as well as unified security table
        if (request.getRole() == Role.SELLER) {
            if (!partnerSellerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                com.stockbridge.accounts.entity.PartnerSeller partnerSeller = com.stockbridge.accounts.entity.PartnerSeller.builder()
                        .organisation(organisation)
                        .fullName(request.getFullName().trim())
                        .email(normalizedEmail)
                        .phone(request.getPhone().trim())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .enabled(true)
                        .build();
                partnerSellerRepository.save(partnerSeller);
            }
        } else if (request.getRole() == Role.BUYER) {
            if (!wholesaleBuyerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                com.stockbridge.accounts.entity.WholesaleBuyer wholesaleBuyer = com.stockbridge.accounts.entity.WholesaleBuyer.builder()
                        .organisation(organisation)
                        .fullName(request.getFullName().trim())
                        .email(normalizedEmail)
                        .phone(request.getPhone().trim())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .enabled(true)
                        .build();
                wholesaleBuyerRepository.save(wholesaleBuyer);
            }
        }

        AppUser user = AppUser.builder()
                .organisation(organisation)
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        AppUser savedUser = userRepository.save(user);

        log.info("Successfully registered user '{}' with role '{}' for organisation '{}' (id: {})",
                savedUser.getEmail(), savedUser.getRole(), organisation.getLegalName(), organisation.getId());

        return mapToUserProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Role targetRole = request.getRole();

        AppUser user;
        if (targetRole != null) {
            user = userRepository.findByEmailAndRole(normalizedEmail, targetRole)
                    .orElseThrow(() -> new BadCredentialsException("No " + targetRole + " account found with email: " + normalizedEmail));
        } else {
            List<AppUser> users = userRepository.findAllByEmail(normalizedEmail);
            if (users.isEmpty()) {
                throw new BadCredentialsException("Invalid email or password");
            }
            // Find user whose password matches
            user = users.stream()
                    .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                    .findFirst()
                    .orElse(users.get(0));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed password authentication attempt for email '{}'", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled. Please contact support.");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String token = jwtService.generateToken(userPrincipal);

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .organisationId(user.getOrganisation().getId())
                .verificationStatus(user.getOrganisation().getVerificationStatus())
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userInfo)
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
        OrganisationSummaryDto orgSummary = OrganisationSummaryDto.builder()
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
                .build();

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
