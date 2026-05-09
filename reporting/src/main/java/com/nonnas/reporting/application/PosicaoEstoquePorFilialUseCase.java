package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.PosicaoEstoqueItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PosicaoEstoquePorFilialUseCase {

    private final RelatorioQueries queries;

    public PosicaoEstoquePorFilialUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<PosicaoEstoqueItem> execute(Filtros f, int page, int size) {
        return queries.posicaoEstoque(f.filialId(), f.categoriaId(), page, size);
    }

    public record Filtros(UUID filialId, UUID categoriaId) {}
}
