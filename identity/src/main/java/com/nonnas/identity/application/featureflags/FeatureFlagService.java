package com.nonnas.identity.application.featureflags;

import com.nonnas.identity.application.ports.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * API da aplicação para consultar feature flags. A regra atual é
 * binária ({@code habilitada}); rolloutPct fica preparado pra gating
 * progressivo no futuro (ex.: ativar pra 10% dos usuários).
 *
 * <p>POC (master doc 14.6 / T18): {@code LGPD_EXCLUSAO_ATIVADA} bloqueia
 * o endpoint {@code DELETE /lgpd/exclusao} quando off (ver {@code LgpdService}).
 */
@Service
public class FeatureFlagService {

    private final FeatureFlagRepository repository;
    private final SecureRandom rng = new SecureRandom();

    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    /**
     * Decide se a flag está ativa pra esta chamada. Considera primeiro o
     * {@code habilitada=false} (kill-switch absoluto). Quando {@code habilitada=true}
     * mas {@code rolloutPct < 100}, decide aleatoriamente — chamadas
     * sucessivas <em>não</em> são determinísticas. Para sticky-rollout por
     * usuário, evolução futura tem que mudar pra hash(usuarioId) % 100.
     */
    public boolean isAtiva(String chave) {
        return repository.findByChave(chave)
                .map(this::decidir)
                .orElse(false);
    }

    private boolean decidir(FeatureFlag flag) {
        if (!flag.habilitada()) return false;
        if (flag.rolloutPct() >= 100) return true;
        if (flag.rolloutPct() <= 0) return false;
        return rng.nextInt(100) < flag.rolloutPct();
    }
}
