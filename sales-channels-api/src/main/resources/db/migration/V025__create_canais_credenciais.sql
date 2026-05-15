-- T-CANAL-01 — Credencial de canal (1 por filial+tipo).
-- O segredo é armazenado cifrado (AES-256-GCM via CryptoService de T16).
-- `merchant_externo_id` é o identificador da loja no canal (ex.: iFood merchantId).

CREATE TABLE canais_credenciais (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canal_tipo            VARCHAR(40)  NOT NULL,
    filial_id             UUID         NOT NULL,
    merchant_externo_id   VARCHAR(120) NOT NULL,
    client_id             VARCHAR(200) NOT NULL,
    client_secret_cifrado TEXT         NOT NULL,
    base_url              VARCHAR(300),
    ativa                 BOOLEAN      NOT NULL DEFAULT TRUE,
    observacao            TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_canais_credenciais_tipo CHECK (canal_tipo IN (
        'IFOOD',
        'NOVENTANOVE_FOOD',
        'KEETA',
        'OPEN_DELIVERY_GENERICO'
    ))
);

-- Uma credencial ativa por (canal, filial). Reativações criam nova linha
-- depois de desativar a anterior — preserva histórico para auditoria.
CREATE UNIQUE INDEX uq_canais_credenciais_ativa
    ON canais_credenciais (canal_tipo, filial_id)
    WHERE ativa = TRUE;

-- Lookup por merchant externo: o adapter de cada canal recebe pedidos
-- já com o `merchant_externo_id` e precisa mapear pra filial Nonnas.
CREATE INDEX idx_canais_credenciais_merchant_externo
    ON canais_credenciais (canal_tipo, merchant_externo_id);
