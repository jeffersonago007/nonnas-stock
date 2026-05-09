-- ════════════════════════════════════════════════════════════════════════
-- T18 — Feature flags placeholder
--
-- Master doc 14.6 / T18: tabela de feature flags + classe FeatureFlagService
-- como prova de conceito. UI de gerenciamento fica para evolução posterior;
-- por enquanto admin troca via SQL direto.
-- ════════════════════════════════════════════════════════════════════════

CREATE TABLE feature_flags (
    chave           VARCHAR(64) PRIMARY KEY,
    descricao       TEXT NOT NULL,
    habilitada      BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_pct     INTEGER NOT NULL DEFAULT 0,                 -- 0..100; gating progressivo no futuro
    criada_em       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizada_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_feature_flags_rollout CHECK (rollout_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_feature_flags_chave_format CHECK (chave ~ '^[a-z][a-z0-9_-]+$')
);

-- Seed da primeira flag — a usada como POC pelo FeatureFlagService.
INSERT INTO feature_flags (chave, descricao, habilitada, rollout_pct)
VALUES ('lgpd-exclusao-ativada',
        'Permite POST /api/v1/lgpd/exclusao executar anonimização. Quando off, retorna 503 explicando que o ciclo de revogação está fora de janela.',
        TRUE, 100);
