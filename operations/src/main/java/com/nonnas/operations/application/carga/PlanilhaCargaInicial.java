package com.nonnas.operations.application.carga;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Modelo intermediário entre o parser e o use case. Linhas da planilha
 * normalizadas, prontas para virar {@link ProcessarCargaInicialUseCase.ItemEntrada}.
 */
public record PlanilhaCargaInicial(
        String hashSha256,
        String nomeArquivo,
        List<Linha> linhas
) {

    public record Linha(
            int numeroLinha,
            UUID insumoId,
            UUID unidadeId,
            String numeroLote,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            LocalDate dataFabricacao,
            LocalDate dataValidade
    ) {}
}
