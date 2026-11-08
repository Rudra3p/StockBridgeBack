package com.stockbridge.accounts.repository;

import com.stockbridge.accounts.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
    boolean existsByGstNumber(String gstNumber);
    Optional<Organisation> findByGstNumber(String gstNumber);
}
