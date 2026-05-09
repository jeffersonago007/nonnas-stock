package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.DivergenciaInventarioItem;
import com.nonnas.reporting.domain.PeriodoFiltro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DivergenciaInventarioUseCase {

    private final RelatorioQueries queries;

    public DivergenciaInventarioUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<DivergenciaInventarioItem> execute(UUID filialId, PeriodoFiltro periodo, int page, int size) {
        return queries.divergenciaInventario(filialId, periodo, page, size);
    }
}
