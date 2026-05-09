package com.nonnas.identity.application.ports;

import com.nonnas.identity.application.featureflags.FeatureFlag;

import java.util.Optional;

public interface FeatureFlagRepository {
    Optional<FeatureFlag> findByChave(String chave);
}
