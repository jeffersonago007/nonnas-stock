package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha do relatório CMV por insumo no período. {@code cmvTotal} =
 * Σ(quantidade × custo unitário vigente da movimentação) — o custo vigente
 * vem direto de {@code items_movimentacao.valor_unitario}, que reflete custo
 * FIFO real (RASTREADO) ou custo médio ponderado (AGREGADOR) no momento da
 * saída (T-CMV-01).
 */
public record CmvPorInsumoItem(
        UUID insumoId,
        String codigo,
        String nome,
        BigDecimal quantidadeVendidaBase,
        BigDecimal cmvTotal,
        BigDecimal custoMedioPeriodo,
        long quantidadeMovimentacoes
) {}
