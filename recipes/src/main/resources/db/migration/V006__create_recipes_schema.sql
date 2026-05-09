-- Recipes: produtos vendáveis, fichas técnicas versionadas e itens.
-- Versionamento por nova linha: cada edição cria nova ficha (versao+1) e
-- desativa a anterior na mesma transação. Histórico imutável.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE produtos_vendaveis (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo      VARCHAR(50)  NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    categoria   VARCHAR(50)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_produtos_vendaveis_codigo UNIQUE (codigo)
);

CREATE INDEX idx_produtos_vendaveis_ativo ON produtos_vendaveis(ativo);

CREATE TABLE fichas_tecnicas (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    produto_vendavel_id  UUID         NOT NULL,
    versao               INT          NOT NULL,
    vigente_desde        TIMESTAMPTZ  NOT NULL,
    vigente_ate          TIMESTAMPTZ,
    ativa                BOOLEAN      NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_fichas_produto FOREIGN KEY (produto_vendavel_id) REFERENCES produtos_vendaveis(id),
    CONSTRAINT uq_fichas_produto_versao UNIQUE (produto_vendavel_id, versao),
    CONSTRAINT chk_fichas_versao_positiva CHECK (versao > 0),
    CONSTRAINT chk_fichas_vigencia CHECK (vigente_ate IS NULL OR vigente_ate >= vigente_desde)
);

-- Garante no máximo uma ficha ativa por produto vendável.
CREATE UNIQUE INDEX uq_fichas_ativa_por_produto
    ON fichas_tecnicas(produto_vendavel_id) WHERE ativa = TRUE;

CREATE INDEX idx_fichas_produto ON fichas_tecnicas(produto_vendavel_id);

CREATE TABLE items_ficha_tecnica (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ficha_tecnica_id   UUID          NOT NULL,
    insumo_id          UUID          NOT NULL,
    unidade_id         UUID          NOT NULL,
    quantidade         NUMERIC(20,4) NOT NULL,
    CONSTRAINT fk_items_ficha FOREIGN KEY (ficha_tecnica_id) REFERENCES fichas_tecnicas(id) ON DELETE CASCADE,
    CONSTRAINT chk_items_ficha_quantidade_positiva CHECK (quantidade > 0)
);

CREATE INDEX idx_items_ficha ON items_ficha_tecnica(ficha_tecnica_id);
CREATE INDEX idx_items_ficha_insumo ON items_ficha_tecnica(insumo_id);
