-- T-LOT-01 (parte catalog): adiciona dias_alerta_vencimento em insumos.
-- O flag controla_validade já existe desde V003 (default TRUE no schema atual).
-- O adendo de lote opcional sugere default FALSE para insumos novos a partir
-- de agora; essa mudança fica para T-LOT-02 no domain (factory `Insumo.novo`)
-- — preservamos default TRUE no schema para não disparar migração silenciosa
-- de insumos existentes.

ALTER TABLE insumos
    ADD COLUMN dias_alerta_vencimento INTEGER NULL;

ALTER TABLE insumos
    ADD CONSTRAINT chk_insumos_dias_alerta_vencimento_range
        CHECK (dias_alerta_vencimento IS NULL
            OR (dias_alerta_vencimento BETWEEN 1 AND 90));

COMMENT ON COLUMN insumos.dias_alerta_vencimento IS
    'Dias antes da validade para disparar alerta de vencimento próximo. '
    'Só faz sentido com controla_validade = true; ignorado caso contrário.';
