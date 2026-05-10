-- Lista de contatos opcional por fornecedor (vendedor, financeiro, suporte etc.).
-- Cada contato tem pelo menos um dos três campos preenchido — validação no
-- domain. CASCADE no delete: se um fornecedor é removido fisicamente do banco,
-- contatos vão junto.

CREATE TABLE fornecedores_contatos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id   UUID         NOT NULL,
    nome            VARCHAR(150),
    email           VARCHAR(150),
    telefone        VARCHAR(30),
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_contatos_fornecedor
        FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id) ON DELETE CASCADE,
    CONSTRAINT chk_contato_pelo_menos_um_campo
        CHECK (nome IS NOT NULL OR email IS NOT NULL OR telefone IS NOT NULL)
);

CREATE INDEX idx_contatos_fornecedor ON fornecedores_contatos(fornecedor_id);
