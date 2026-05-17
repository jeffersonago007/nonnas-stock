package com.nonnas.reporting.application.ports;

import com.nonnas.reporting.domain.CmvPorCanalItem;
import com.nonnas.reporting.domain.CmvPorInsumoItem;
import com.nonnas.reporting.domain.CmvPorProdutoItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de CMV (Custo da Mercadoria Vendida) — T-CMV-01. Cross-context
 * read-only via SQL nativo (ADR 0010): lê movimentações de
 * {@code SAIDA_VENDA} em inventory-core + metadados em catalog/recipes +
 * cabeçalho de pedido em sales-channels-api.
 *
 * <p>O custo unitário gravado em cada {@code items_movimentacao.valor_unitario}
 * no momento da saída é a fonte da verdade — preserva histórico mesmo se
 * o custo médio do insumo mudar depois.
 */
public interface CmvQueries {

    /** CMV agrupado por insumo. Filtra por filial opcionalmente. */
    List<CmvPorInsumoItem> cmvPorInsumo(Instant de, Instant ate, UUID filialId);

    /** CMV agrupado por produto vendável. */
    List<CmvPorProdutoItem> cmvPorProduto(Instant de, Instant ate, UUID filialId);

    /** CMV agrupado por canal de venda (iFood, 99Food, Keeta, Open Delivery genérico). */
    List<CmvPorCanalItem> cmvPorCanal(Instant de, Instant ate, UUID filialId);
}
