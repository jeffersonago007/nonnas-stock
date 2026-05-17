-- T-QOL-03 — Cripto AES-256-GCM em colunas PII de pedidos de canal.
--
-- (1) Expande length pra acomodar ciphertext base64. Plaintext típico
--     "Maria Silva" (~12 chars) vira ~88 chars base64 após AES-GCM
--     (12 bytes IV + 12 bytes payload + 16 bytes tag = 40 bytes → ~56 chars
--     base64). 500 cobre nomes longos com folga.
--
-- (2) Limpa dados em claro existentes. Demo pré-T-QOL-03 não tem cliente
--     real (pedidos DEMO-*). Optamos por TRUNCATE em vez de tentar cifrar
--     em SQL (sem acesso ao CryptoService) — o Jefferson autorizou em
--     T-QOL-03 dado o estado pré-go-live. Itens e eventos caem por CASCADE
--     ou DELETE explícito.

DELETE FROM eventos_canais;
DELETE FROM pedidos_canais;

ALTER TABLE pedidos_canais
    ALTER COLUMN cliente_nome     TYPE VARCHAR(500),
    ALTER COLUMN cliente_telefone TYPE VARCHAR(500);
