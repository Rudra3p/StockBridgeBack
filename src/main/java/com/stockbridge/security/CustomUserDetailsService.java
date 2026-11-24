package com.stockbridge.security;

import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.repository.AdminUserRepository;
import com.stockbridge.accounts.repository.PartnerSellerRepository;
import com.stockbridge.accounts.repository.UserRepository;
import com.stockbridge.accounts.repository.WholesaleBuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final WholesaleBuyerRepository wholesaleBuyerRepository;
    private final PartnerSellerRepository partnerSellerRepository;
    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username != null && username.contains(":")) {
            String[] parts = username.split(":", 2);
            String email = parts[0].trim().toLowerCase();
            try {
                Role role = Role.valueOf(parts[1].trim().toUpperCase());
                return loadUserByEmailAndRole(email, role);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String email = username != null ? username.trim().toLowerCase() : "";

        // Check 3 separate tables in logical order
        return wholesaleBuyerRepository.findByEmailIgnoreCase(email)
                .map(UserPrincipal::new)
                .or(() -> partnerSellerRepository.findByEmailIgnoreCase(email).map(UserPrincipal::new))
                .or(() -> adminUserRepository.findByEmailIgnoreCase(email).map(UserPrincipal::new))
                .or(() -> userRepository.findAllByEmail(email).stream().findFirst().map(UserPrincipal::new))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public UserDetails loadUserByEmailAndRole(String email, Role role) throws UsernameNotFoundException {
        String cleanEmail = email.trim().toLowerCase();

        if (role == Role.BUYER) {
            return wholesaleBuyerRepository.findByEmailIgnoreCase(cleanEmail)
                    .map(UserPrincipal::new)
                    .or(() -> userRepository.findByEmailAndRole(cleanEmail, Role.BUYER).map(UserPrincipal::new))
                    .orElseThrow(() -> new UsernameNotFoundException("No Buyer account found with email: " + cleanEmail));
        }

        if (role == Role.SELLER) {
            return partnerSellerRepository.findByEmailIgnoreCase(cleanEmail)
                    .map(UserPrincipal::new)
                    .or(() -> userRepository.findByEmailAndRole(cleanEmail, Role.SELLER).map(UserPrincipal::new))
                    .orElseThrow(() -> new UsernameNotFoundException("No Seller account found with email: " + cleanEmail));
        }

        if (role == Role.ADMIN) {
            return adminUserRepository.findByEmailIgnoreCase(cleanEmail)
                    .map(UserPrincipal::new)
                    .or(() -> userRepository.findByEmailAndRole(cleanEmail, Role.ADMIN).map(UserPrincipal::new))
                    .orElseThrow(() -> new UsernameNotFoundException("No Administrator account found with email: " + cleanEmail));
        }

        return loadUserByUsername(cleanEmail);
    }
}
