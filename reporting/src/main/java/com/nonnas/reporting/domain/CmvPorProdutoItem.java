package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linha do relatório CMV por produto vendável (FABRICADO ou REVENDA).
 * Une 2 caminhos de venda: {@code documento_origem_tipo='FICHA_TECNICA'}
 * (resolve via ficha → produto) e {@code 'PRODUTO_REVENDA'} (id já é o produto).
 */
public record CmvPorProdutoItem(
        UUID produtoVendavelId,
        String codigo,
        String nome,
        BigDecimal quantidadeVendida,
        BigDecimal cmvTotal,
        long quantidadeMovimentacoes
) {}
