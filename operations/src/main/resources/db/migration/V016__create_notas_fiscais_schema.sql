-- T20: Lançamento de Nota Fiscal.
-- Tabelas: nota fiscal + itens + de-para fornecedor → insumo (aprendizado
-- automático de matching para notas seguintes do mesmo emitente).

CREATE TABLE notas_fiscais (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id            UUID          NOT NULL,
    filial_id                UUID          NOT NULL,
    numero                   VARCHAR(20)   NOT NULL,
    serie                    VARCHAR(10)   NOT NULL,
    chave_nfe                VARCHAR(44),
    data_emissao             TIMESTAMPTZ   NOT NULL,
    data_lancamento          TIMESTAMPTZ   NOT NULL,
    valor_total              NUMERIC(20,2) NOT NULL,
    observacao               TEXT,
    created_by_usuario_id    UUID          NOT NULL,
    movimentacao_entrada_id  UUID          NOT NULL,
    created_at               TIMESTAMPTZ   NOT NULL,
    updated_at               TIMESTAMPTZ   NOT NULL,
    -- Idempotência: a mesma NF-e (44 dígitos) só pode ser lançada uma vez.
    -- Notas manuais (sem chave) não disparam o constraint, portanto entrada
    -- duplicada em modo manual é responsabilidade do operador checar.
    CONSTRAINT uq_notas_fiscais_chave UNIQUE (chave_nfe),
    CONSTRAINT chk_notas_fiscais_valor_positivo CHECK (valor_total >= 0),
    CONSTRAINT chk_notas_fiscais_chave_44digitos
        CHECK (chave_nfe IS NULL OR chave_nfe ~ '^[0-9]{44}$')
);

CREATE INDEX idx_notas_fiscais_fornecedor ON notas_fiscais(fornecedor_id);
CREATE INDEX idx_notas_fiscais_filial     ON notas_fiscais(filial_id);
CREATE INDEX idx_notas_fiscais_data_emi   ON notas_fiscais(data_emissao DESC);

CREATE TABLE notas_fiscais_itens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nota_fiscal_id      UUID          NOT NULL,
    insumo_id           UUID          NOT NULL,
    codigo_fornecedor   VARCHAR(60),
    descricao_origem    VARCHAR(500)  NOT NULL,
    quantidade          NUMERIC(20,4) NOT NULL,
    unidade_medida_id   UUID          NOT NULL,
    valor_unitario      NUMERIC(20,4) NOT NULL,
    valor_total         NUMERIC(20,2) NOT NULL,
    lote                VARCHAR(60),
    data_validade       DATE,
    created_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_nfi_nota FOREIGN KEY (nota_fiscal_id) REFERENCES notas_fiscais(id) ON DELETE CASCADE,
    CONSTRAINT chk_nfi_qtd_positiva     CHECK (quantidade > 0),
    CONSTRAINT chk_nfi_valor_unit_pos   CHECK (valor_unitario >= 0)
);

CREATE INDEX idx_nfi_nota   ON notas_fiscais_itens(nota_fiscal_id);
CREATE INDEX idx_nfi_insumo ON notas_fiscais_itens(insumo_id);

-- De-para de matching automático. Aprendizado: quando o usuário confirma
-- lançamento de uma nota, registra/atualiza par (fornecedor, cProd) → insumo.
-- Próxima nota desse mesmo fornecedor já vem com sugestão de match resolvida.
CREATE TABLE fornecedor_insumo_depara (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id        UUID          NOT NULL,
    codigo_fornecedor    VARCHAR(60)   NOT NULL,
    insumo_id            UUID          NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL,
    last_used_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_depara UNIQUE (fornecedor_id, codigo_fornecedor)
);

CREATE INDEX idx_depara_lookup ON fornecedor_insumo_depara(fornecedor_id, codigo_fornecedor);
