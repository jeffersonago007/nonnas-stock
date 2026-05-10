-- T-LOT-01 (parte inventory-core): introduz `tipo` no lote para suportar o
-- regime opcional de rastreabilidade descrito no adendo
-- (docs/adendo-lote-opcional.md).
--
-- Originalmente criada como V020 no commit 0fcf984; renomeada para V021 em
-- T-LOT-09 porque colidia com catalog/V020 quando alerts/operations
-- consolidam os dois classpaths no Flyway. STATUS.md detalha.
--
-- Adaptação ao modelo atual: a tabela `lotes` é universal (sem filial_id);
-- saldos por filial vivem em `saldos_lotes`. Logo, o lote AGREGADOR é único
-- por INSUMO (não por insumo+filial), e cada filial mantém seu saldo nesse
-- mesmo agregador via `saldos_lotes(lote_agregador_id, filial_id)`.
--
-- Lotes existentes ficam como RASTREADO. Constraints de "validade obrigatória"
-- e "número obrigatório" só se aplicam a AGREGADOR para evitar quebra de
-- legacy (lotes RASTREADO antigos podem ter sido criados sem `data_validade`
-- via carga inicial mais antiga). A invariante estrita "RASTREADO precisa
-- de validade" fica protegida pelo factory do domain (`Lote.novoRastreado`)
-- — schema só impede combinações claramente inválidas para AGREGADOR.

-- 1. Nova coluna tipo (default RASTREADO preserva todos os lotes existentes).
ALTER TABLE lotes
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RASTREADO';

ALTER TABLE lotes
    ADD CONSTRAINT chk_lotes_tipo_valido
        CHECK (tipo IN ('RASTREADO', 'AGREGADOR'));

COMMENT ON COLUMN lotes.tipo IS
    'RASTREADO: lote real com numero e validade, fluxo FEFO na saída. '
    'AGREGADOR: lote único por insumo (controla_validade=false), saída direta '
    'do saldo agregado por filial em saldos_lotes.';

-- 2. Lote agregador não tem número nem datas (validade/fabricação).
--    Drop NOT NULL em numero_lote para acomodar.
ALTER TABLE lotes
    ALTER COLUMN numero_lote DROP NOT NULL;

-- 3. Constraints garantindo que AGREGADOR é vazio nesses 3 campos.
ALTER TABLE lotes
    ADD CONSTRAINT chk_lotes_agregador_sem_numero
        CHECK (tipo <> 'AGREGADOR' OR numero_lote IS NULL);

ALTER TABLE lotes
    ADD CONSTRAINT chk_lotes_agregador_sem_validade
        CHECK (tipo <> 'AGREGADOR' OR data_validade IS NULL);

ALTER TABLE lotes
    ADD CONSTRAINT chk_lotes_agregador_sem_fabricacao
        CHECK (tipo <> 'AGREGADOR' OR data_fabricacao IS NULL);

-- 4. Único agregador por insumo. Índice parcial é a forma idiomática em PG.
CREATE UNIQUE INDEX uq_lotes_agregador_por_insumo
    ON lotes (insumo_id)
    WHERE tipo = 'AGREGADOR';

-- 5. Lookup acelerado pra path quente do BuscarOuCriarLoteAgregadorUseCase.
CREATE INDEX idx_lotes_agregador_lookup
    ON lotes (insumo_id, tipo)
    WHERE tipo = 'AGREGADOR';
