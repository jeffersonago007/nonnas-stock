package com.nonnas.identity.application.featureflags;

import java.time.Instant;

/**
 * Snapshot de uma feature flag. Hoje só admin troca via SQL; UI de
 * gerenciamento fica para evolução posterior (master doc 14.6 / T18 — POC).
 */
public record FeatureFlag(
        String chave,
        String descricao,
        boolean habilitada,
        int rolloutPct,
        Instant criadaEm,
        Instant atualizadaEm
) {

    /** Chaves bem-conhecidas — referenciadas pelo código pra evitar typo. */
    public static final class Chaves {
        public static final String LGPD_EXCLUSAO_ATIVADA = "lgpd-exclusao-ativada";

        private Chaves() {}
    }
}
