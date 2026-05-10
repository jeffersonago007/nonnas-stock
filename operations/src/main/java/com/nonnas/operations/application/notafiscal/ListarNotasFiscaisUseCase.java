package com.nonnas.operations.application.notafiscal;

import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.NotaFiscal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.List;
import java.util.UUID;

@Service
public class ListarNotasFiscaisUseCase {

    private final NotaFiscalRepository repository;

    public ListarNotasFiscaisUseCase(NotaFiscalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotaFiscal> execute(UUID filialId, Instant emissaoDe,
                                    Instant emissaoAte, int page, int size) {
        return repository.findFiltered(filialId, emissaoDe, emissaoAte, page, size);
    }
}
