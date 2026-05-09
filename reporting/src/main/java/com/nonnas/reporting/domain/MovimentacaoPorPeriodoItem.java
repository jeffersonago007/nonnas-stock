package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record MovimentacaoPorPeriodoItem(
        UUID filialId,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        String tipoMovimentacao,
        long quantidadeMovimentacoes,
        BigDecimal quantidadeTotal,
        BigDecimal valorTotal
) {}
