-- ════════════════════════════════════════════════════════════════════════
-- T16 — Hardening de segurança e LGPD
--
-- Adiciona 4 tabelas (audit_log, tokens_revogados, aceites_termos,
-- usuarios_2fa) ao schema identity. Tentativas de login já estão cobertas
-- por colunas em `usuarios` desde V001 (tentativas_falhas, bloqueado_ate).
-- ════════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─── audit_log ──────────────────────────────────────────────────────────
-- Registra eventos não-CRUD: login, logout, alteração de perfil, exercício
-- de direitos LGPD. Eventos CRUD ficam em tabelas envers (T17/T18).
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    actor_id        UUID NULL,                      -- usuário que executou (null = sistema)
    actor_email     VARCHAR(255) NULL,              -- snapshot, evita JOIN se ator deletado
    event_type      VARCHAR(64) NOT NULL,           -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, PERFIL_ALTERADO, etc.
    target_kind     VARCHAR(64) NULL,               -- USUARIO, FILIAL, etc.
    target_id       UUID NULL,
    request_ip      VARCHAR(64) NULL,
    request_ua      VARCHAR(512) NULL,
    metadata        TEXT NULL,                      -- JSON serializado em string (evita dep Hypersistence Utils)
    CONSTRAINT chk_audit_event_type_nonempty CHECK (LENGTH(TRIM(event_type)) > 0)
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_actor       ON audit_log (actor_id, occurred_at DESC);
CREATE INDEX idx_audit_log_event_type  ON audit_log (event_type, occurred_at DESC);

-- ─── tokens_revogados ───────────────────────────────────────────────────
-- Blacklist de access tokens JWT. Logout insere o JTI (jti claim) com
-- expiração igual ao exp do token. Filter de auth checa antes de aceitar.
-- Limpeza: registros com expira_em < NOW() podem ser purgados (job futuro).
CREATE TABLE tokens_revogados (
    jti             VARCHAR(64) PRIMARY KEY,
    usuario_id      UUID NOT NULL,
    revogado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expira_em       TIMESTAMP WITH TIME ZONE NOT NULL,  -- mesmo exp do JWT
    motivo          VARCHAR(64) NOT NULL,                -- LOGOUT, TROCA_SENHA, ADMIN_FORCE
    CONSTRAINT fk_tokens_revogados_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_tokens_revogados_motivo CHECK (motivo IN ('LOGOUT', 'TROCA_SENHA', 'ADMIN_FORCE', 'TWO_FA_HABILITADO'))
);

CREATE INDEX idx_tokens_revogados_expira_em ON tokens_revogados (expira_em);
CREATE INDEX idx_tokens_revogados_usuario   ON tokens_revogados (usuario_id);

-- ─── aceites_termos ─────────────────────────────────────────────────────
-- LGPD Art. 8º: prova de consentimento por usuário e versão de termos.
-- Versão dos termos vive em código (constantes) ou em outra tabela futura.
CREATE TABLE aceites_termos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID NOT NULL,
    versao_termos   VARCHAR(32) NOT NULL,
    versao_privacidade VARCHAR(32) NOT NULL,
    aceito_em       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    request_ip      VARCHAR(64) NULL,
    CONSTRAINT fk_aceites_termos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_aceites_termos_usuario_versao UNIQUE (usuario_id, versao_termos)
);

CREATE INDEX idx_aceites_termos_usuario ON aceites_termos (usuario_id);

-- ─── usuarios_2fa ───────────────────────────────────────────────────────
-- TOTP secret + backup codes para 2FA. Secret é criptografado em coluna
-- (CamposSensiveisConverter) — NÃO armazenamos em plain.
-- backup_codes_hash: array de hashes SHA-256 dos códigos one-shot.
CREATE TABLE usuarios_2fa (
    usuario_id          UUID PRIMARY KEY,
    secret_cifrado      TEXT NOT NULL,                  -- AES-GCM base64(iv||ciphertext||tag)
    backup_codes_hash   TEXT[] NOT NULL DEFAULT '{}',   -- SHA-256 hex de cada código
    confirmado          BOOLEAN NOT NULL DEFAULT FALSE, -- false = só setup, true = ativo
    confirmado_em       TIMESTAMP WITH TIME ZONE NULL,
    criado_em           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_usuarios_2fa_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Índice parcial: só interessa varrer registros confirmados em queries
-- de gating ("este admin já tem 2FA?").
CREATE INDEX idx_usuarios_2fa_confirmados ON usuarios_2fa (usuario_id) WHERE confirmado = TRUE;
