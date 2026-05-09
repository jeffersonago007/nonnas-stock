package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record CurvaABCItem(
        UUID filialId,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        BigDecimal quantidadeTotal,
        BigDecimal valorTotal,
        BigDecimal percentualAcumulado,
        ClasseABC classe
) {}
