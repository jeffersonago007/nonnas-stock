package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.featureflags.FeatureFlag;
import com.nonnas.identity.application.ports.FeatureFlagRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class FeatureFlagRepositoryImpl implements FeatureFlagRepository {

    private final SpringDataFeatureFlagRepository jpa;

    FeatureFlagRepositoryImpl(SpringDataFeatureFlagRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<FeatureFlag> findByChave(String chave) {
        return jpa.findById(chave).map(e -> new FeatureFlag(
                e.getChave(),
                e.getDescricao(),
                e.isHabilitada(),
                e.getRolloutPct(),
                e.getCriadaEm(),
                e.getAtualizadaEm()));
    }
}
