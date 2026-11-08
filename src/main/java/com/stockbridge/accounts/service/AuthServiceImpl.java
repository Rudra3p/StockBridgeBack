package com.stockbridge.accounts.service;

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
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' already exists.");
        }

        String normalizedGst = request.getGstNumber().trim().toUpperCase();
        if (organisationRepository.existsByGstNumber(normalizedGst)) {
            throw new DuplicateResourceException("An organisation with GST number '" + normalizedGst + "' already exists.");
        }

        // 1. Create Organisation with PENDING status
        Organisation organisation = Organisation.builder()
                .legalName(request.getLegalName().trim())
                .orgType(request.getOrgType())
                .gstNumber(normalizedGst)
                .addressLine(request.getAddressLine().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .pinCode(request.getPinCode().trim())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        Organisation savedOrg = organisationRepository.save(organisation);

        // 2. Create User linked to Organisation
        AppUser user = AppUser.builder()
                .organisation(savedOrg)
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        AppUser savedUser = userRepository.save(user);

        log.info("Successfully registered user '{}' with role '{}' for organisation '{}' (id: {})",
                savedUser.getEmail(), savedUser.getRole(), savedOrg.getLegalName(), savedOrg.getId());

        return mapToUserProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(userPrincipal);
            AppUser user = userPrincipal.getAppUser();

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

        } catch (AuthenticationException e) {
            log.warn("Failed authentication attempt for email '{}'", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String email) {
        AppUser user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToUserProfileResponse(user);
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
