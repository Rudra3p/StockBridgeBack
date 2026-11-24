package com.stockbridge.security;

import com.stockbridge.accounts.entity.AdminUser;
import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.PartnerSeller;
import com.stockbridge.accounts.entity.Role;
import com.stockbridge.accounts.entity.WholesaleBuyer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final UUID organisationId;
    private final Collection<? extends GrantedAuthority> authorities;
    private final AppUser appUser;

    public UserPrincipal(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.organisationId = user.getOrganisation() != null ? user.getOrganisation().getId() : null;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        this.appUser = user;
    }

    public UserPrincipal(WholesaleBuyer buyer) {
        this.id = buyer.getId();
        this.email = buyer.getEmail();
        this.password = buyer.getPasswordHash();
        this.role = Role.BUYER;
        this.enabled = buyer.isEnabled();
        this.organisationId = buyer.getOrganisation() != null ? buyer.getOrganisation().getId() : null;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"));
        this.appUser = AppUser.builder()
                .id(buyer.getId())
                .email(buyer.getEmail())
                .fullName(buyer.getFullName())
                .phone(buyer.getPhone())
                .passwordHash(buyer.getPasswordHash())
                .role(Role.BUYER)
                .organisation(buyer.getOrganisation())
                .enabled(buyer.isEnabled())
                .build();
    }

    public UserPrincipal(PartnerSeller seller) {
        this.id = seller.getId();
        this.email = seller.getEmail();
        this.password = seller.getPasswordHash();
        this.role = Role.SELLER;
        this.enabled = seller.isEnabled();
        this.organisationId = seller.getOrganisation() != null ? seller.getOrganisation().getId() : null;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELLER"));
        this.appUser = AppUser.builder()
                .id(seller.getId())
                .email(seller.getEmail())
                .fullName(seller.getFullName())
                .phone(seller.getPhone())
                .passwordHash(seller.getPasswordHash())
                .role(Role.SELLER)
                .organisation(seller.getOrganisation())
                .enabled(seller.isEnabled())
                .build();
    }

    public UserPrincipal(AdminUser admin) {
        this.id = admin.getId();
        this.email = admin.getEmail();
        this.password = admin.getPasswordHash();
        this.role = Role.ADMIN;
        this.enabled = admin.isEnabled();
        this.organisationId = null;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        this.appUser = AppUser.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .phone(admin.getPhone())
                .passwordHash(admin.getPasswordHash())
                .role(Role.ADMIN)
                .enabled(admin.isEnabled())
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
