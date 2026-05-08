-- Inventory core: lotes, saldos por (lote, filial), movimentações imutáveis e itens.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE lotes (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insumo_id         UUID         NOT NULL,
    fornecedor_id     UUID,
    nota_fiscal_id    UUID,
    numero_lote       VARCHAR(100) NOT NULL,
    data_fabricacao   DATE,
    data_validade     DATE,
    valor_unitario    NUMERIC(20,4) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_lotes_valor_nao_negativo CHECK (valor_unitario >= 0),
    CONSTRAINT chk_lotes_validade_pos_fabricacao
        CHECK (data_validade IS NULL OR data_fabricacao IS NULL OR data_validade >= data_fabricacao)
);

CREATE INDEX idx_lotes_insumo            ON lotes(insumo_id);
CREATE INDEX idx_lotes_validade          ON lotes(data_validade NULLS LAST);
CREATE INDEX idx_lotes_insumo_validade   ON lotes(insumo_id, data_validade NULLS LAST);

CREATE TABLE saldos_lotes (
    lote_id          UUID         NOT NULL,
    filial_id        UUID         NOT NULL,
    quantidade_base  NUMERIC(20,4) NOT NULL DEFAULT 0,
    atualizado_em    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (lote_id, filial_id),
    CONSTRAINT fk_saldos_lote FOREIGN KEY (lote_id) REFERENCES lotes(id)
);

CREATE INDEX idx_saldos_filial ON saldos_lotes(filial_id);

CREATE TABLE movimentacoes (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filial_id                UUID         NOT NULL,
    usuario_id               UUID         NOT NULL,
    tipo                     VARCHAR(40)  NOT NULL,
    data_movimentacao        TIMESTAMPTZ  NOT NULL,
    documento_origem_tipo    VARCHAR(40),
    documento_origem_id      UUID,
    observacao               TEXT,
    gerou_negativo           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_movimentacoes_tipo CHECK (tipo IN (
        'ENTRADA_NF', 'ENTRADA_AJUSTE', 'ENTRADA_TRANSFERENCIA',
        'ENTRADA_DEVOLUCAO_CLIENTE', 'ENTRADA_CARGA_INICIAL',
        'SAIDA_VENDA', 'SAIDA_AJUSTE', 'SAIDA_TRANSFERENCIA',
        'SAIDA_PERDA', 'SAIDA_QUEBRA', 'SAIDA_VENCIMENTO'
    ))
);

CREATE INDEX idx_movimentacoes_filial_data   ON movimentacoes(filial_id, data_movimentacao DESC);
CREATE INDEX idx_movimentacoes_documento     ON movimentacoes(documento_origem_tipo, documento_origem_id);

CREATE TABLE items_movimentacao (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movimentacao_id         UUID         NOT NULL,
    insumo_id               UUID         NOT NULL,
    lote_id                 UUID         NOT NULL,
    unidade_lancamento_id   UUID         NOT NULL,
    quantidade_lancada      NUMERIC(20,4) NOT NULL,
    quantidade_base         NUMERIC(20,4) NOT NULL,
    valor_unitario          NUMERIC(20,4) NOT NULL,
    CONSTRAINT fk_items_movimentacao FOREIGN KEY (movimentacao_id) REFERENCES movimentacoes(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_lote          FOREIGN KEY (lote_id) REFERENCES lotes(id),
    CONSTRAINT chk_items_qtd_lancada_positiva CHECK (quantidade_lancada > 0)
);

CREATE INDEX idx_items_movimentacao ON items_movimentacao(movimentacao_id);
CREATE INDEX idx_items_lote         ON items_movimentacao(lote_id);
CREATE INDEX idx_items_insumo       ON items_movimentacao(insumo_id);
