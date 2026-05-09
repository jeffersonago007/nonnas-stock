package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.RupturaIminenteItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RupturaIminenteUseCase {

    private final RelatorioQueries queries;

    public RupturaIminenteUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<RupturaIminenteItem> execute(UUID filialId, int page, int size) {
        return queries.rupturaIminente(filialId, page, size);
    }
}
