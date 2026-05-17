package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.CmvQueries;
import com.nonnas.reporting.domain.CmvPorCanalItem;
import com.nonnas.reporting.domain.CmvPorInsumoItem;
import com.nonnas.reporting.domain.CmvPorProdutoItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra as 3 perspectivas de CMV (T-CMV-01). Delegação direta para o
 * port — não há regra de negócio adicional aqui; mantemos o use case para
 * preservar o padrão dos outros relatórios e permitir, no futuro, hooks de
 * cache/audit sem mexer no controller.
 */
@Service
public class CmvUseCase {

    private final CmvQueries queries;

    public CmvUseCase(CmvQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<CmvPorInsumoItem> porInsumo(Instant de, Instant ate, UUID filialId) {
        return queries.cmvPorInsumo(de, ate, filialId);
    }

    @Transactional(readOnly = true)
    public List<CmvPorProdutoItem> porProduto(Instant de, Instant ate, UUID filialId) {
        return queries.cmvPorProduto(de, ate, filialId);
    }

    @Transactional(readOnly = true)
    public List<CmvPorCanalItem> porCanal(Instant de, Instant ate, UUID filialId) {
        return queries.cmvPorCanal(de, ate, filialId);
    }
}
