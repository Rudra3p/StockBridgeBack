package com.stockbridge.security;

import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username != null && username.contains(":")) {
            String[] parts = username.split(":", 2);
            String email = parts[0].trim().toLowerCase();
            try {
                Role role = Role.valueOf(parts[1].trim().toUpperCase());
                return userRepository.findByEmailAndRole(email, role)
                        .map(UserPrincipal::new)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email + " and role: " + role));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<AppUser> users = userRepository.findAllByEmail(username.trim().toLowerCase());
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }
        return new UserPrincipal(users.get(0));
    }

    public UserDetails loadUserByEmailAndRole(String email, Role role) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmailAndRole(email.trim().toLowerCase(), role)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email + " and role: " + role));
        return new UserPrincipal(user);
    }
}
