package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record PosicaoEstoqueItem(
        UUID filialId,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        BigDecimal saldoTotal,
        BigDecimal valorEstoque,
        long quantidadeLotes
) {}
