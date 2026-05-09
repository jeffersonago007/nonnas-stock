package com.nonnas.reporting.application;

import com.nonnas.reporting.application.ports.RelatorioQueries;
import com.nonnas.reporting.domain.VencimentoProximoItem;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VencimentoProximoUseCase {

    private static final int JANELA_PADRAO_DIAS = 30;
    private static final int JANELA_MAX_DIAS = 365;

    private final RelatorioQueries queries;

    public VencimentoProximoUseCase(RelatorioQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<VencimentoProximoItem> execute(UUID filialId, Integer diasJanela, int page, int size) {
        int dias = diasJanela == null ? JANELA_PADRAO_DIAS : diasJanela;
        if (dias <= 0) {
            throw new ValidationException("Janela de vencimento deve ser positiva.");
        }
        if (dias > JANELA_MAX_DIAS) {
            throw new ValidationException("Janela máxima de vencimento é " + JANELA_MAX_DIAS + " dias.");
        }
        return queries.vencimentoProximo(filialId, dias, page, size);
    }
}
