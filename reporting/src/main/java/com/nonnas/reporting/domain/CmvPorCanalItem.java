package com.nonnas.reporting.domain;

import java.math.BigDecimal;

/**
 * Linha do relatório CMV por canal de venda. Cruza
 * {@code pedidos_canais.movimentacao_id} → movimentações pra somar CMV, e
 * usa {@code pedidos_canais.valor_liquido} (T-CANAL-06) pra receita líquida
 * recebida. Margem bruta = {@code receitaLiquidaTotal − cmvTotal} (antes
 * de comissão de marketplace, que vem em settlement separado).
 *
 * <p>{@code canal} é o {@code CanalTipo.name()}, exposto como string pra
 * evitar dep direta sales-channels-api ↔ reporting (ADR 0010).
 */
public record CmvPorCanalItem(
        String canal,
        long quantidadePedidos,
        BigDecimal receitaLiquidaTotal,
        BigDecimal cmvTotal,
        BigDecimal margemBruta
) {}
