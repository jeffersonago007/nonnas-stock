-- Operations: transferência entre filiais (state machine), ajuste manual e carga inicial.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE transferencias (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filial_origem_id      UUID         NOT NULL,
    filial_destino_id     UUID         NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    solicitado_por        UUID         NOT NULL,
    aprovado_por          UUID,
    enviado_por           UUID,
    recebido_por          UUID,
    data_solicitacao      TIMESTAMPTZ  NOT NULL,
    data_aprovacao        TIMESTAMPTZ,
    data_envio            TIMESTAMPTZ,
    data_recebimento      TIMESTAMPTZ,
    observacao            TEXT,
    mov_saida_id          UUID,
    mov_entrada_id        UUID,
    motivo_cancelamento   TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_transferencias_status CHECK (
        status IN ('SOLICITADA','APROVADA','EM_TRANSITO','RECEBIDA','CANCELADA')
    ),
    CONSTRAINT chk_transferencias_filiais_distintas CHECK (filial_origem_id <> filial_destino_id)
);

CREATE INDEX idx_transferencias_status         ON transferencias(status);
CREATE INDEX idx_transferencias_filial_origem  ON transferencias(filial_origem_id, status);
CREATE INDEX idx_transferencias_filial_destino ON transferencias(filial_destino_id, status);

CREATE TABLE itens_transferencia (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transferencia_id         UUID          NOT NULL,
    insumo_id                UUID          NOT NULL,
    unidade_id               UUID          NOT NULL,
    quantidade_solicitada    NUMERIC(20,4) NOT NULL,
    quantidade_recebida      NUMERIC(20,4),
    CONSTRAINT fk_itens_transferencia FOREIGN KEY (transferencia_id) REFERENCES transferencias(id) ON DELETE CASCADE,
    CONSTRAINT chk_itens_transferencia_qtd_solicitada_pos CHECK (quantidade_solicitada > 0),
    CONSTRAINT chk_itens_transferencia_qtd_recebida_nn   CHECK (quantidade_recebida IS NULL OR quantidade_recebida >= 0)
);

CREATE INDEX idx_itens_transferencia ON itens_transferencia(transferencia_id);

CREATE TABLE ajustes_estoque (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filial_id                UUID          NOT NULL,
    insumo_id                UUID          NOT NULL,
    unidade_id               UUID          NOT NULL,
    quantidade_diff          NUMERIC(20,4) NOT NULL,
    motivo                   TEXT          NOT NULL,
    status                   VARCHAR(25)   NOT NULL,
    requer_aprovacao         BOOLEAN       NOT NULL,
    solicitado_por           UUID          NOT NULL,
    aprovado_por             UUID,
    data_solicitacao         TIMESTAMPTZ   NOT NULL,
    data_aprovacao           TIMESTAMPTZ,
    mov_id                   UUID,
    origem_transferencia_id  UUID,
    rejeicao_motivo          TEXT,
    created_at               TIMESTAMPTZ   NOT NULL,
    updated_at               TIMESTAMPTZ   NOT NULL,
    CONSTRAINT chk_ajustes_estoque_status CHECK (
        status IN ('PENDENTE_APROVACAO','APROVADO','REJEITADO')
    ),
    CONSTRAINT chk_ajustes_estoque_diff_nao_zero CHECK (quantidade_diff <> 0)
);

CREATE INDEX idx_ajustes_estoque_filial      ON ajustes_estoque(filial_id, status);
CREATE INDEX idx_ajustes_estoque_pendentes   ON ajustes_estoque(status) WHERE status = 'PENDENTE_APROVACAO';
CREATE INDEX idx_ajustes_estoque_origem_tr   ON ajustes_estoque(origem_transferencia_id) WHERE origem_transferencia_id IS NOT NULL;

CREATE TABLE cargas_iniciais (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filial_id                UUID         NOT NULL,
    hash_planilha            VARCHAR(64)  NOT NULL,
    nome_arquivo             VARCHAR(255) NOT NULL,
    registros_processados    INT          NOT NULL DEFAULT 0,
    registros_falhos         INT          NOT NULL DEFAULT 0,
    solicitado_por           UUID         NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_cargas_iniciais_hash UNIQUE (hash_planilha)
);

CREATE INDEX idx_cargas_iniciais_filial ON cargas_iniciais(filial_id, created_at DESC);
