package com.nonnas.reporting.application.ports;

import com.nonnas.reporting.domain.CurvaABCItem;
import com.nonnas.reporting.domain.DivergenciaInventarioItem;
import com.nonnas.reporting.domain.MovimentacaoPorPeriodoItem;
import com.nonnas.reporting.domain.PeriodoFiltro;
import com.nonnas.reporting.domain.PosicaoEstoqueItem;
import com.nonnas.reporting.domain.RupturaIminenteItem;
import com.nonnas.reporting.domain.VencimentoProximoItem;

import java.util.List;
import java.util.UUID;

/**
 * Port read-only para o módulo reporting. Implementação faz SQL nativo
 * cross-schema sem importar classes Java de outros bounded contexts (ADR 0010).
 */
public interface RelatorioQueries {

    List<PosicaoEstoqueItem> posicaoEstoque(UUID filialId, UUID categoriaId, int page, int size);

    List<CurvaABCItem> curvaAbc(UUID filialId, int page, int size);

    List<RupturaIminenteItem> rupturaIminente(UUID filialId, int page, int size);

    List<VencimentoProximoItem> vencimentoProximo(UUID filialId, int diasJanela, int page, int size);

    List<MovimentacaoPorPeriodoItem> movimentacaoPorPeriodo(
            UUID filialId, PeriodoFiltro periodo, String tipoMovimentacao, int page, int size);

    List<DivergenciaInventarioItem> divergenciaInventario(
            UUID filialId, PeriodoFiltro periodo, int page, int size);

    /**
     * Refresh concorrente das views materializadas. Usa
     * {@code REFRESH MATERIALIZED VIEW CONCURRENTLY} para não bloquear leituras.
     */
    void refreshViewsMaterializadas();
}
