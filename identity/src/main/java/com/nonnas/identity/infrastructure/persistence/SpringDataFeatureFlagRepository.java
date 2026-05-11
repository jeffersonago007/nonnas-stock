package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataFeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {
}
