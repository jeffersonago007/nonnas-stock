-- T-QOL-03 — Cripto AES-256-GCM em contatos de fornecedor (PII LGPD).
--
-- Mesma estratégia da V030 de sales-channels-api: expande length pra
-- ciphertext base64 e limpa dados em claro pré-T-QOL-03 (autorizado em
-- estado pré-go-live). Numerada V031 (não V030) para evitar colisão
-- Flyway no classpath consolidado — o app importa migrations dos dois
-- módulos e a versão precisa ser única globalmente.

DELETE FROM fornecedores_contatos;

ALTER TABLE fornecedores_contatos
    ALTER COLUMN nome     TYPE VARCHAR(500),
    ALTER COLUMN email    TYPE VARCHAR(500),
    ALTER COLUMN telefone TYPE VARCHAR(500);
