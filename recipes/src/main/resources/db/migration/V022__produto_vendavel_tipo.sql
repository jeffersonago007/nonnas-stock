-- Adiciona o tipo do produto vendável:
--   FABRICADO  → exige ficha técnica, baixa N insumos via FEFO (lógica original T05).
--   REVENDA    → vincula direto a 1 insumo (Coca-Cola, sorvete fechado, bala etc.),
--                baixa 1:1 do estoque, sem ficha técnica.
-- Default 'FABRICADO' preserva semântica anterior (todos os produtos existentes).
-- Sem FK física para catalog.insumos: cross-context é validado em runtime (padrão T03 InsumoFilial).

ALTER TABLE produtos_vendaveis
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'FABRICADO',
    ADD COLUMN insumo_revenda_id UUID;

ALTER TABLE produtos_vendaveis
    ADD CONSTRAINT chk_produtos_vendaveis_tipo
        CHECK (tipo IN ('FABRICADO', 'REVENDA')),
    ADD CONSTRAINT chk_produtos_vendaveis_revenda_requer_insumo
        CHECK (tipo <> 'REVENDA' OR insumo_revenda_id IS NOT NULL),
    ADD CONSTRAINT chk_produtos_vendaveis_fabricado_sem_insumo
        CHECK (tipo <> 'FABRICADO' OR insumo_revenda_id IS NULL);

CREATE INDEX idx_produtos_vendaveis_insumo_revenda
    ON produtos_vendaveis(insumo_revenda_id)
    WHERE insumo_revenda_id IS NOT NULL;
