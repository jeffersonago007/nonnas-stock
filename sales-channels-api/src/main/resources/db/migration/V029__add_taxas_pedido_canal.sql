-- T-CANAL-06 — Discriminação de taxas no pedido de canal.
--
-- `taxa_entrega`  = soma das taxas de tipo DELIVERY_FEE em `otherFees[]`
--                   (Order Open Delivery v1.0.1).
-- `taxa_servico`  = soma das taxas de tipo SERVICE_FEE em `otherFees[]`.
-- `valor_liquido` = valor_total - taxa_entrega - taxa_servico
--                   (receita bruta do merchant, antes de comissão de
--                   marketplace — comissão não aparece no Order; vem em
--                   settlement separado, fora do POC).
--
-- TIP (gorjeta) é capturada no payload canônico mas NÃO entra no valor
-- líquido — gorjeta vai pro entregador/garçom, não pro restaurante.

ALTER TABLE pedidos_canais
    ADD COLUMN taxa_entrega  NUMERIC(20,4) NOT NULL DEFAULT 0,
    ADD COLUMN taxa_servico  NUMERIC(20,4) NOT NULL DEFAULT 0,
    ADD COLUMN valor_liquido NUMERIC(20,4) NOT NULL DEFAULT 0;

-- Backfill: pedidos pré-T-CANAL-06 não tinham taxas extraídas. Assume
-- taxas zero e valor_liquido = valor_total (como vinha sendo exibido).
UPDATE pedidos_canais
   SET valor_liquido = valor_total
 WHERE valor_liquido = 0
   AND valor_total > 0;

ALTER TABLE pedidos_canais
    ADD CONSTRAINT chk_pedidos_canais_taxa_entrega_nao_negativa
        CHECK (taxa_entrega >= 0),
    ADD CONSTRAINT chk_pedidos_canais_taxa_servico_nao_negativa
        CHECK (taxa_servico >= 0),
    ADD CONSTRAINT chk_pedidos_canais_valor_liquido_nao_negativo
        CHECK (valor_liquido >= 0);
