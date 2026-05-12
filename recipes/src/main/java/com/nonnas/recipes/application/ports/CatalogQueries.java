package com.nonnas.recipes.application.ports;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Port para consultas pontuais a catalog que recipes não importa via Maven.
 * Padrão ADR 0010 (SQL nativo cross-context).
 */
public interface CatalogQueries {

    /**
     * @return UUID da unidade base do insumo. {@code Optional.empty()} se o
     *         insumo não existe — usar pra validar existência também.
     */
    Optional<UUID> findUnidadeBaseDoInsumo(UUID insumoId);

    /**
     * Resolve insumo + saldo da filial em uma query — usado pra montar a
     * tela Vendas unificada e validar antes de auto-promover.
     */
    Optional<InsumoComSaldo> findInsumoComSaldo(UUID insumoId, UUID filialId);

    /**
     * Lista insumos ativos com saldo > 0 na filial cujo id NÃO está em
     * {@code insumosJaVinculados} (produtos REVENDA existentes). Usado pela
     * tela Vendas pra mostrar insumos órfãos prontos pra venda direta.
     */
    List<InsumoComSaldo> findInsumosOrfaosComSaldo(UUID filialId, Set<UUID> insumosJaVinculados);

    record InsumoComSaldo(
            UUID insumoId,
            String codigo,
            String nome,
            UUID unidadeBaseId,
            String unidadeBaseCodigo,
            BigDecimal saldo
    ) {}

    /**
     * Conta vendas (SAIDA_VENDA) por produto vendável nos últimos 30 dias.
     * Cobre ambos os caminhos:
     * <ul>
     *   <li>FABRICADO: lookup via {@code fichas_tecnicas} pelo {@code documento_origem_id}.</li>
     *   <li>REVENDA: {@code documento_origem_id} já é o produto.</li>
     * </ul>
     * Produtos sem venda no período não aparecem no map.
     */
    Map<UUID, Integer> contarVendasPorProdutoUltimos30Dias(UUID filialId);
}
