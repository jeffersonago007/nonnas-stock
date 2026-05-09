-- Alerts: configuração com escopo flexível e disparos com auto-resolução.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE alertas_config (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo         VARCHAR(40)  NOT NULL,
    insumo_id    UUID,                                    -- NULL = todos os insumos
    filial_id    UUID,                                    -- NULL = todas as filiais
    threshold    NUMERIC(20,4),                           -- semântica varia por tipo (validada no domínio)
    ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
    prioridade   INT          NOT NULL DEFAULT 0,         -- desempate quando múltiplas configs casam
    observacao   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_alertas_config_tipo CHECK (tipo IN (
        'ESTOQUE_MINIMO_PERCENTUAL',
        'ESTOQUE_MINIMO_ABSOLUTO',
        'VENCIMENTO_PROXIMO_DIAS',
        'RUPTURA'
    )),
    -- RUPTURA dispensa threshold; demais tipos exigem.
    CONSTRAINT chk_alertas_config_threshold CHECK (
        (tipo = 'RUPTURA') OR (threshold IS NOT NULL AND threshold > 0)
    )
);

CREATE INDEX idx_alertas_config_escopo
    ON alertas_config(tipo, insumo_id, filial_id) WHERE ativo = TRUE;

CREATE TABLE alertas_disparados (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id             UUID         NOT NULL,
    tipo                  VARCHAR(40)  NOT NULL,
    insumo_id             UUID         NOT NULL,
    filial_id             UUID         NOT NULL,
    lote_id               UUID,                  -- usado apenas em VENCIMENTO_PROXIMO_DIAS
    status                VARCHAR(20)  NOT NULL,
    saldo_no_disparo      NUMERIC(20,4),
    detalhe               TEXT,
    data_disparo          TIMESTAMPTZ  NOT NULL,
    data_resolucao        TIMESTAMPTZ,
    visualizado_em        TIMESTAMPTZ,
    visualizado_por       UUID,
    resolvido_por         UUID,
    CONSTRAINT fk_alertas_disparados_config FOREIGN KEY (config_id) REFERENCES alertas_config(id),
    CONSTRAINT chk_alertas_disparados_status CHECK (
        status IN ('ATIVO','RESOLVIDO_AUTO','RESOLVIDO_MANUAL')
    )
);

CREATE INDEX idx_alertas_disparados_ativos
    ON alertas_disparados(insumo_id, filial_id) WHERE status = 'ATIVO';
CREATE INDEX idx_alertas_disparados_status_data
    ON alertas_disparados(status, data_disparo DESC);
CREATE INDEX idx_alertas_disparados_config
    ON alertas_disparados(config_id, status);
-- Garante no máximo 1 alerta ATIVO por (config, lote) — idempotência do job de vencimento.
CREATE UNIQUE INDEX uq_alertas_disparados_ativo_lote
    ON alertas_disparados(config_id, lote_id) WHERE status = 'ATIVO' AND lote_id IS NOT NULL;
-- Para tipos sem lote, idempotência por (config, insumo, filial) ativo.
CREATE UNIQUE INDEX uq_alertas_disparados_ativo_sem_lote
    ON alertas_disparados(config_id, insumo_id, filial_id) WHERE status = 'ATIVO' AND lote_id IS NULL;
