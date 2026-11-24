package com.stockbridge.accounts.repository;

import com.stockbridge.accounts.entity.PartnerSeller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerSellerRepository extends JpaRepository<PartnerSeller, UUID> {
    Optional<PartnerSeller> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
