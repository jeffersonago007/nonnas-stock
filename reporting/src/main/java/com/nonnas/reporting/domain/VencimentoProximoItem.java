package com.nonnas.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VencimentoProximoItem(
        UUID filialId,
        UUID insumoId,
        UUID loteId,
        String insumoCodigo,
        String insumoNome,
        String numeroLote,
        LocalDate dataValidade,
        long diasParaVencer,
        BigDecimal saldo,
        BigDecimal valorUnitario
) {}
