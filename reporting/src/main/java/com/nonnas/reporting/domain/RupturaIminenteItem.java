package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record RupturaIminenteItem(
        UUID filialId,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        BigDecimal saldoTotal,
        BigDecimal estoqueMinimo,
        BigDecimal pontoPedido,
        SituacaoRuptura situacao
) {}
