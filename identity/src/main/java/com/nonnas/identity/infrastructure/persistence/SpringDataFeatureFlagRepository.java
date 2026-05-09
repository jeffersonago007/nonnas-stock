package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {
}
