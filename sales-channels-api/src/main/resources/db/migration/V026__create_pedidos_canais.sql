-- T-CANAL-01 — Pedido recebido de canal externo, já em forma canônica
-- (Open Delivery subset). `pedido_externo_id` é a chave do canal, único
-- por (canal, pedido_externo_id) — bloqueia reentrada via idempotência
-- a nível de schema.
--
-- `payload_canonico_json` guarda o PedidoVendaCanonico serializado para
-- auditoria/debug. `payload_bruto_json` guarda o payload original do canal
-- (iFood Order, Open Delivery Order, ...) para forensics — útil quando o
-- mapper acusa divergência.

CREATE TABLE pedidos_canais (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canal_tipo              VARCHAR(40)   NOT NULL,
    pedido_externo_id       VARCHAR(120)  NOT NULL,
    display_id              VARCHAR(80),
    filial_id               UUID          NOT NULL,
    credencial_id           UUID          NOT NULL,
    status                  VARCHAR(40)   NOT NULL,
    valor_total             NUMERIC(20,4) NOT NULL,
    moeda                   VARCHAR(3)    NOT NULL DEFAULT 'BRL',
    cliente_nome            VARCHAR(200),
    cliente_telefone        VARCHAR(40),
    payload_canonico_json   JSONB         NOT NULL,
    payload_bruto_json      JSONB,
    movimentacao_id         UUID,
    erro_processamento      TEXT,
    recebido_em             TIMESTAMPTZ   NOT NULL,
    processado_em           TIMESTAMPTZ,
    concluido_em            TIMESTAMPTZ,
    cancelado_em            TIMESTAMPTZ,
    CONSTRAINT chk_pedidos_canais_status CHECK (status IN (
        'RECEBIDO',
        'EM_PROCESSAMENTO',
        'CONFIRMADO_ESTOQUE',
        'CONCLUIDO',
        'CANCELADO',
        'FALHA'
    )),
    CONSTRAINT fk_pedidos_canais_credencial FOREIGN KEY (credencial_id)
        REFERENCES canais_credenciais(id)
);

-- Idempotência: mesmo pedido externo nunca é processado 2 vezes.
CREATE UNIQUE INDEX uq_pedidos_canais_externo
    ON pedidos_canais (canal_tipo, pedido_externo_id);

CREATE INDEX idx_pedidos_canais_filial_status
    ON pedidos_canais (filial_id, status);
CREATE INDEX idx_pedidos_canais_recebido_em
    ON pedidos_canais (recebido_em DESC);

-- Itens do pedido canônico (subset Open Delivery). `produto_vendavel_id`
-- só é resolvido em T-CANAL-04 quando o use case ProcessarPedidoCanal
-- materializar a baixa; até lá fica NULL e o `external_code` é a chave
-- bruta de mapeamento.
CREATE TABLE itens_pedido_canal (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_canal_id       UUID          NOT NULL,
    sequencia             INT           NOT NULL,
    external_code         VARCHAR(120),
    nome                  VARCHAR(300)  NOT NULL,
    quantidade            NUMERIC(20,4) NOT NULL,
    unidade               VARCHAR(20)   NOT NULL,
    preco_unitario        NUMERIC(20,4) NOT NULL,
    preco_total           NUMERIC(20,4) NOT NULL,
    observacao            TEXT,
    produto_vendavel_id   UUID,
    CONSTRAINT fk_itens_pedido_canal_pedido FOREIGN KEY (pedido_canal_id)
        REFERENCES pedidos_canais(id) ON DELETE CASCADE,
    CONSTRAINT chk_itens_pedido_canal_qtd CHECK (quantidade > 0),
    CONSTRAINT uq_itens_pedido_canal_seq UNIQUE (pedido_canal_id, sequencia)
);

CREATE INDEX idx_itens_pedido_canal_pedido ON itens_pedido_canal (pedido_canal_id);
