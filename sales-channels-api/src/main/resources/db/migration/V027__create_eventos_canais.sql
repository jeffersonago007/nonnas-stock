-- T-CANAL-01 — Log de eventos brutos recebidos do canal (Open Delivery
-- segue o ciclo PLC/CFM/DSP/CON/CAN). Cada evento tem um `event_id`
-- externo único por canal — UNIQUE bloqueia reprocessamento (polling
-- pode entregar o mesmo evento mais de uma vez).
--
-- `pedido_canal_id` pode ser NULL no primeiro evento de um pedido novo
-- (preenchido depois que o pedido canônico for criado).

CREATE TABLE eventos_canais (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canal_tipo         VARCHAR(40)  NOT NULL,
    event_id_externo   VARCHAR(120) NOT NULL,
    tipo_evento        VARCHAR(40)  NOT NULL,
    pedido_externo_id  VARCHAR(120),
    pedido_canal_id    UUID,
    payload_json       JSONB        NOT NULL,
    recebido_em        TIMESTAMPTZ  NOT NULL,
    acknowledged_em    TIMESTAMPTZ,
    processado_em      TIMESTAMPTZ,
    erro               TEXT,
    CONSTRAINT chk_eventos_canais_tipo CHECK (tipo_evento IN (
        'PEDIDO_CRIADO',
        'PEDIDO_CONFIRMADO',
        'PEDIDO_DESPACHADO',
        'PEDIDO_CONCLUIDO',
        'PEDIDO_CANCELADO',
        'OUTRO'
    )),
    CONSTRAINT fk_eventos_canais_pedido FOREIGN KEY (pedido_canal_id)
        REFERENCES pedidos_canais(id)
);

-- Idempotência: mesmo event_id do canal nunca é processado 2 vezes.
CREATE UNIQUE INDEX uq_eventos_canais_externo
    ON eventos_canais (canal_tipo, event_id_externo);

CREATE INDEX idx_eventos_canais_pedido_externo
    ON eventos_canais (canal_tipo, pedido_externo_id, recebido_em DESC);
CREATE INDEX idx_eventos_canais_nao_processados
    ON eventos_canais (recebido_em ASC) WHERE processado_em IS NULL;
