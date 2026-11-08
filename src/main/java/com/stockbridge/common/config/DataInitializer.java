package com.stockbridge.common.config;

import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.OrgType;
import com.stockbridge.accounts.entity.Organisation;
import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.entity.VerificationStatus;
import com.stockbridge.accounts.repository.OrganisationRepository;
import com.stockbridge.accounts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@stockbridge.local";
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user not found. Seeding default ADMIN user and organisation...");

            String adminGst = "ADMIN0000000001";
            Organisation adminOrg = organisationRepository.findByGstNumber(adminGst)
                    .orElseGet(() -> organisationRepository.save(
                            Organisation.builder()
                                    .legalName("StockBridge Platform Admin Org")
                                    .orgType(OrgType.OTHER)
                                    .gstNumber(adminGst)
                                    .addressLine("StockBridge HQ, Tech Park")
                                    .city("Bengaluru")
                                    .state("Karnataka")
                                    .pinCode("560001")
                                    .verificationStatus(VerificationStatus.APPROVED)
                                    .build()
                    ));

            AppUser adminUser = AppUser.builder()
                    .organisation(adminOrg)
                    .fullName("StockBridge Administrator")
                    .email(adminEmail)
                    .phone("+919876543210")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();

            userRepository.save(adminUser);
            log.info("Default ADMIN user seeded successfully with email '{}' (Default password: admin123)", adminEmail);
        } else {
            log.info("Admin user '{}' already exists.", adminEmail);
        }
    }
}
