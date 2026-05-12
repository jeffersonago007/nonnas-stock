-- Converte reporting.mv_ruptura_iminente de view materializada para view comum,
-- eliminando o atraso de até 30 min do refresh cron. Para o porte Nonnas
-- (dezenas/centenas de insumos por filial) o cálculo on-the-fly é trivial.
-- A mv_curva_abc continua materializada (agregação rolling de 90 dias é pesada).

DROP MATERIALIZED VIEW IF EXISTS reporting.mv_ruptura_iminente CASCADE;

CREATE VIEW reporting.mv_ruptura_iminente AS
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
