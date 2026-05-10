package com.nonnas.nfeimporter.domain;

import java.math.BigDecimal;

/**
 * Item extraído de uma tag {@code <det>} do XML NF-e. Mantém apenas os
 * campos relevantes para localizar/criar Insumo e gerar a entrada no
 * estoque — impostos e demais detalhes fiscais ficam fora deste escopo.
 *
 * <p>{@code codigoFornecedor} corresponde ao {@code cProd} do emitente
 * (uma chave que faz sentido só na perspectiva daquele fornecedor) e é
 * o que alimenta a tabela {@code fornecedor_insumo_depara} para
 * aprendizado de matching nas notas seguintes.
 */
public record ItemLido(
        int numero,
        String codigoFornecedor,
        String descricao,
        String ncm,
        String unidadeComercial,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal
) {}
