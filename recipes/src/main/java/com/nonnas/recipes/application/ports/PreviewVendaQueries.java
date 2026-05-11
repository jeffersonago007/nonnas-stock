package com.nonnas.recipes.application.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port para metadados cross-context usados no preview de venda. Lê de
 * catalog (insumos, unidades_medida) e inventory-core (lotes, saldos_lotes)
 * via SQL nativo, sem importar classes desses módulos — padrão ADR 0010.
 */
public interface PreviewVendaQueries {

    Map<UUID, InsumoMeta> fetchInsumoMeta(Collection<UUID> insumoIds, UUID filialId);

    Map<UUID, LoteMeta> fetchLoteMeta(Collection<UUID> loteIds);

    record InsumoMeta(
            String nome,
            String unidadeBaseCodigo,
            boolean controlaValidade,
            BigDecimal saldoAtual
    ) {}

    record LoteMeta(
            String numeroLote,
            LocalDate dataValidade
    ) {}
}
