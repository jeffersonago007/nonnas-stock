package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.MovimentacaoPorPeriodoItem;
import com.nonnas.reporting.domain.PeriodoFiltro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MovimentacaoPorPeriodoUseCase {

    private final RelatorioQueries queries;

    public MovimentacaoPorPeriodoUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoPorPeriodoItem> execute(Filtros f, int page, int size) {
        return queries.movimentacaoPorPeriodo(
                f.filialId(), f.periodo(), f.tipoMovimentacao(), page, size);
    }

    public record Filtros(UUID filialId, PeriodoFiltro periodo, String tipoMovimentacao) {}
}
