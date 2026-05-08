-- Catalog bounded context: categorias, unidades de medida, conversões, fornecedores, insumos, insumos_filiais.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE categorias_insumo (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_pai_id   UUID,
    nome               VARCHAR(255) NOT NULL,
    ativa              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_categorias_pai FOREIGN KEY (categoria_pai_id) REFERENCES categorias_insumo(id),
    CONSTRAINT chk_categorias_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0)
);

CREATE TABLE unidades_medida (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo      VARCHAR(20)  NOT NULL,
    nome        VARCHAR(100) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    ativa       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_unidades_codigo UNIQUE (codigo),
    CONSTRAINT chk_unidades_tipo CHECK (tipo IN ('PESO', 'VOLUME', 'UNIDADE')),
    CONSTRAINT chk_unidades_codigo_nao_vazio CHECK (LENGTH(TRIM(codigo)) > 0)
);

CREATE TABLE fornecedores (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    razao_social  VARCHAR(255) NOT NULL,
    cnpj          VARCHAR(14)  NOT NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_fornecedores_cnpj UNIQUE (cnpj),
    CONSTRAINT chk_fornecedores_cnpj_format CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT chk_fornecedores_razao_social_nao_vazia CHECK (LENGTH(TRIM(razao_social)) > 0)
);

CREATE TABLE insumos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo              VARCHAR(50)  NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    categoria_id        UUID         NOT NULL,
    unidade_base_id     UUID         NOT NULL,
    controla_lote       BOOLEAN      NOT NULL DEFAULT TRUE,
    controla_validade   BOOLEAN      NOT NULL DEFAULT TRUE,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_insumos_codigo UNIQUE (codigo),
    CONSTRAINT fk_insumos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_insumo(id),
    CONSTRAINT fk_insumos_unidade   FOREIGN KEY (unidade_base_id) REFERENCES unidades_medida(id),
    CONSTRAINT chk_insumos_codigo_nao_vazio CHECK (LENGTH(TRIM(codigo)) > 0),
    CONSTRAINT chk_insumos_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0)
);

CREATE INDEX idx_insumos_categoria ON insumos(categoria_id);
CREATE INDEX idx_insumos_unidade   ON insumos(unidade_base_id);

CREATE TABLE conversoes_unidade (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insumo_id            UUID,
    unidade_origem_id    UUID         NOT NULL,
    unidade_destino_id   UUID         NOT NULL,
    fator                NUMERIC(20,8) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_conversoes_origem  FOREIGN KEY (unidade_origem_id)  REFERENCES unidades_medida(id),
    CONSTRAINT fk_conversoes_destino FOREIGN KEY (unidade_destino_id) REFERENCES unidades_medida(id),
    CONSTRAINT fk_conversoes_insumo  FOREIGN KEY (insumo_id)          REFERENCES insumos(id),
    CONSTRAINT chk_conversoes_fator_positivo CHECK (fator > 0),
    CONSTRAINT chk_conversoes_unidades_diferentes CHECK (unidade_origem_id <> unidade_destino_id)
);

-- Global conversions are unique by (origem, destino) where insumo_id IS NULL.
-- Per-insumo conversions are unique by (origem, destino, insumo_id).
CREATE UNIQUE INDEX uq_conversoes_globais
    ON conversoes_unidade(unidade_origem_id, unidade_destino_id)
    WHERE insumo_id IS NULL;

CREATE UNIQUE INDEX uq_conversoes_por_insumo
    ON conversoes_unidade(unidade_origem_id, unidade_destino_id, insumo_id)
    WHERE insumo_id IS NOT NULL;

CREATE INDEX idx_conversoes_origem_destino ON conversoes_unidade(unidade_origem_id, unidade_destino_id);
CREATE INDEX idx_conversoes_insumo         ON conversoes_unidade(insumo_id);

CREATE TABLE insumos_filiais (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insumo_id       UUID         NOT NULL,
    filial_id       UUID         NOT NULL,
    estoque_minimo  NUMERIC(20,4) NOT NULL DEFAULT 0,
    estoque_maximo  NUMERIC(20,4),
    ponto_pedido    NUMERIC(20,4),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_insumos_filiais_insumo FOREIGN KEY (insumo_id) REFERENCES insumos(id),
    -- FK to identity.filiais will be added in T09 when migrations are unified.
    CONSTRAINT uq_insumos_filiais UNIQUE (insumo_id, filial_id),
    CONSTRAINT chk_insumos_filiais_minimo_nao_negativo CHECK (estoque_minimo >= 0)
);

CREATE INDEX idx_insumos_filiais_filial ON insumos_filiais(filial_id);
