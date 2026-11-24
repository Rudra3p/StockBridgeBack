package com.stockbridge.accounts.repository;

import com.stockbridge.accounts.entity.AppUser;
import com.stockbridge.accounts.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndRole(String email, Role role);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailAndRole(String email, Role role);
    List<AppUser> findAllByEmail(String email);
}
