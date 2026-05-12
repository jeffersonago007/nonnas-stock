package com.nonnas.operations.application.notafiscal;

import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.NotaFiscal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarNotasFiscaisUseCase {

    private final NotaFiscalRepository repository;

    public ListarNotasFiscaisUseCase(NotaFiscalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotaFiscal> execute(NotaFiscalRepository.Filtros filtros, int page, int size) {
        return repository.findFiltered(filtros, page, size);
    }
}
