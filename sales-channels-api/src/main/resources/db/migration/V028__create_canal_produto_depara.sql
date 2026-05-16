-- T-CANAL-04 — De-para por canal: mapeia externalCode (SKU/merchantSku
-- vindo do canal) para nosso ProdutoVendavel. Sem FK física para
-- produtos_vendaveis (cross-context recipes — padrão T03/ADR 0010,
-- validado em runtime no use case).
--
-- Filial é opcional: NULL = mapeamento global do canal; UUID = mapeamento
-- específico daquela filial (operador pode ter SKUs diferentes por loja).
-- Partial unique index garante no máximo 1 mapeamento por
-- (canal, externalCode, filial) — incluindo NULL.

CREATE TABLE canal_produto_depara (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canal_tipo            VARCHAR(40)  NOT NULL,
    external_code         VARCHAR(120) NOT NULL,
    filial_id             UUID,
    produto_vendavel_id   UUID         NOT NULL,
    observacao            TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_canal_produto_depara_canal CHECK (canal_tipo IN (
        'IFOOD',
        'NOVENTANOVE_FOOD',
        'KEETA',
        'OPEN_DELIVERY_GENERICO'
    ))
);

-- Unicidade global do mapeamento — diferente trato para NULL vs not-NULL
-- em filial_id porque o Postgres considera NULL distinto de NULL em
-- UNIQUE não-parcial.
CREATE UNIQUE INDEX uq_canal_produto_depara_com_filial
    ON canal_produto_depara (canal_tipo, external_code, filial_id)
    WHERE filial_id IS NOT NULL;
CREATE UNIQUE INDEX uq_canal_produto_depara_global
    ON canal_produto_depara (canal_tipo, external_code)
    WHERE filial_id IS NULL;

-- Lookup principal: dado um externalCode do canal, encontre o produto.
-- Resolução: tenta primeiro (canal, code, filial), depois (canal, code, NULL).
CREATE INDEX idx_canal_produto_depara_lookup
    ON canal_produto_depara (canal_tipo, external_code);
CREATE INDEX idx_canal_produto_depara_produto
    ON canal_produto_depara (produto_vendavel_id);
