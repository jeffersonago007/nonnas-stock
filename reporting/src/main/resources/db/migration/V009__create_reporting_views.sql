-- Reporting: schema isolado com views materializadas para queries pesadas
-- (curva ABC e ruptura iminente) e índices auxiliares para queries diretas
-- (posição, vencimento, movimentação por período, divergência).
--
-- ADR 0010: módulo reporting consome tabelas de outros bounded contexts via
-- SQL nativo cross-schema. As MVs trazem UNIQUE INDEX para suportar
-- REFRESH MATERIALIZED VIEW CONCURRENTLY (sem lock total na leitura).

CREATE SCHEMA IF NOT EXISTS reporting;

-- =====================================================================
-- Curva ABC: classifica insumos por consumo (saídas) dos últimos 90 dias,
-- por (filial, insumo). Pesos pelo valor monetário consumido.
--   A = primeiros 80% do consumo acumulado
--   B = próximos 15%
--   C = restante
-- A janela de 90 dias é rolling — REFRESH atualiza o período.
-- =====================================================================
CREATE MATERIALIZED VIEW reporting.mv_curva_abc AS
WITH consumo AS (
    SELECT
        m.filial_id,
        im.insumo_id,
        SUM(im.quantidade_base)                          AS quantidade_total,
        SUM(im.quantidade_base * im.valor_unitario)      AS valor_total
    FROM movimentacoes m
    JOIN items_movimentacao im ON im.movimentacao_id = m.id
    WHERE m.tipo IN ('SAIDA_VENDA', 'SAIDA_AJUSTE', 'SAIDA_TRANSFERENCIA',
                     'SAIDA_PERDA', 'SAIDA_QUEBRA', 'SAIDA_VENCIMENTO')
      AND m.data_movimentacao >= NOW() - INTERVAL '90 days'
    GROUP BY m.filial_id, im.insumo_id
),
classificada AS (
    SELECT
        filial_id,
        insumo_id,
        quantidade_total,
        valor_total,
        SUM(valor_total) OVER (PARTITION BY filial_id) AS valor_filial,
        SUM(valor_total) OVER (
            PARTITION BY filial_id
            ORDER BY valor_total DESC, insumo_id
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS acumulado
    FROM consumo
)
SELECT
    filial_id,
    insumo_id,
    quantidade_total,
    valor_total,
    valor_filial,
    CASE WHEN valor_filial = 0 THEN 0
         ELSE acumulado / valor_filial
    END AS percentual_acumulado,
    CASE
        WHEN valor_filial = 0                      THEN 'C'
        WHEN acumulado / valor_filial <= 0.80      THEN 'A'
        WHEN acumulado / valor_filial <= 0.95      THEN 'B'
        ELSE                                            'C'
    END AS classe
FROM classificada;

CREATE UNIQUE INDEX uq_mv_curva_abc_filial_insumo
    ON reporting.mv_curva_abc(filial_id, insumo_id);
CREATE INDEX idx_mv_curva_abc_classe
    ON reporting.mv_curva_abc(filial_id, classe);

-- =====================================================================
-- Ruptura iminente: insumos com saldo total na filial = 0, abaixo do
-- ponto_pedido ou abaixo do estoque_minimo configurado em insumos_filiais.
-- Apenas insumos com vínculo ativo na filial.
-- =====================================================================
CREATE MATERIALIZED VIEW reporting.mv_ruptura_iminente AS
WITH saldo_atual AS (
    SELECT
        l.insumo_id,
        sl.filial_id,
        COALESCE(SUM(sl.quantidade_base), 0) AS saldo_total
    FROM lotes l
    JOIN saldos_lotes sl ON sl.lote_id = l.id
    GROUP BY l.insumo_id, sl.filial_id
)
SELECT
    if_.insumo_id,
    if_.filial_id,
    COALESCE(s.saldo_total, 0)               AS saldo_total,
    if_.estoque_minimo,
    if_.ponto_pedido,
    CASE
        WHEN COALESCE(s.saldo_total, 0) = 0
            THEN 'RUPTURA_TOTAL'
        WHEN if_.ponto_pedido IS NOT NULL
             AND COALESCE(s.saldo_total, 0) <= if_.ponto_pedido
            THEN 'ABAIXO_PONTO_PEDIDO'
        WHEN if_.estoque_minimo > 0
             AND COALESCE(s.saldo_total, 0) <= if_.estoque_minimo
            THEN 'ABAIXO_MINIMO'
        ELSE 'NORMAL'
    END AS situacao
FROM insumos_filiais if_
LEFT JOIN saldo_atual s
       ON s.insumo_id = if_.insumo_id
      AND s.filial_id = if_.filial_id
WHERE if_.ativo = TRUE
  AND (
        COALESCE(s.saldo_total, 0) = 0
     OR (if_.ponto_pedido IS NOT NULL
            AND COALESCE(s.saldo_total, 0) <= if_.ponto_pedido)
     OR (if_.estoque_minimo > 0
            AND COALESCE(s.saldo_total, 0) <= if_.estoque_minimo)
      );

CREATE UNIQUE INDEX uq_mv_ruptura_filial_insumo
    ON reporting.mv_ruptura_iminente(filial_id, insumo_id);
CREATE INDEX idx_mv_ruptura_situacao
    ON reporting.mv_ruptura_iminente(filial_id, situacao);

-- =====================================================================
-- Índices auxiliares para queries diretas (não-materializadas).
-- =====================================================================

-- Acelera divergencia_inventario por período (ajustes APROVADOS).
CREATE INDEX IF NOT EXISTS idx_ajustes_aprovados_periodo
    ON ajustes_estoque(data_aprovacao DESC, filial_id, insumo_id)
    WHERE status = 'APROVADO';

-- Acelera movimentacao_por_periodo (filtros por filial+data+tipo).
CREATE INDEX IF NOT EXISTS idx_movimentacoes_filial_tipo_data
    ON movimentacoes(filial_id, tipo, data_movimentacao DESC);
