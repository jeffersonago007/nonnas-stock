package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.CurvaABCItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CurvaABCUseCase {

    private final RelatorioQueries queries;

    public CurvaABCUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<CurvaABCItem> execute(UUID filialId, int page, int size) {
        return queries.curvaAbc(filialId, page, size);
    }
}
