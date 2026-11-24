package com.stockbridge.accounts.repository;

import com.stockbridge.accounts.entity.WholesaleBuyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WholesaleBuyerRepository extends JpaRepository<WholesaleBuyer, UUID> {
    Optional<WholesaleBuyer> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
