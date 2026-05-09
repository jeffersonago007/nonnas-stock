package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record DivergenciaInventarioItem(
        UUID filialId,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        long quantidadeAjustes,
        BigDecimal quantidadeDiffPositiva,
        BigDecimal quantidadeDiffNegativa,
        BigDecimal quantidadeDiffLiquida
) {}
