-- Identity bounded context: empresas, filiais, usuários, refresh tokens, histórico de senhas.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE empresas (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    razao_social  VARCHAR(255) NOT NULL,
    cnpj          VARCHAR(14)  NOT NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_empresas_cnpj UNIQUE (cnpj),
    CONSTRAINT chk_empresas_razao_social_nao_vazia CHECK (LENGTH(TRIM(razao_social)) > 0),
    CONSTRAINT chk_empresas_cnpj_format CHECK (cnpj ~ '^[0-9]{14}$')
);

CREATE TABLE filiais (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id   UUID         NOT NULL,
    nome         VARCHAR(255) NOT NULL,
    cnpj         VARCHAR(14)  NOT NULL,
    endereco     TEXT,
    ativa        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_filiais_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT uq_filiais_cnpj UNIQUE (cnpj),
    CONSTRAINT chk_filiais_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0),
    CONSTRAINT chk_filiais_cnpj_format CHECK (cnpj ~ '^[0-9]{14}$')
);

CREATE INDEX idx_filiais_empresa_id ON filiais(empresa_id);

CREATE TABLE usuarios (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filial_id          UUID,
    nome               VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    senha_hash         VARCHAR(72)  NOT NULL,
    perfil             VARCHAR(20)  NOT NULL,
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    tentativas_falhas  INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate      TIMESTAMPTZ,
    travada            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_usuarios_filial FOREIGN KEY (filial_id) REFERENCES filiais(id),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT chk_usuarios_email_format CHECK (email ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$'),
    CONSTRAINT chk_usuarios_perfil CHECK (perfil IN ('ADMIN', 'GERENTE', 'OPERADOR', 'CONSULTA')),
    CONSTRAINT chk_usuarios_tentativas_nao_negativa CHECK (tentativas_falhas >= 0),
    CONSTRAINT chk_usuarios_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0)
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_filial_id ON usuarios(filial_id);

CREATE TABLE refresh_tokens (
    jti          UUID PRIMARY KEY,
    family_id    UUID         NOT NULL,
    parent_jti   UUID,
    usuario_id   UUID         NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_refresh_tokens_family_id  ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_usuario_id ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

CREATE TABLE historico_senhas (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID         NOT NULL,
    senha_hash   VARCHAR(72)  NOT NULL,
    criada_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_historico_senhas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_historico_senhas_usuario ON historico_senhas(usuario_id, criada_em DESC);
