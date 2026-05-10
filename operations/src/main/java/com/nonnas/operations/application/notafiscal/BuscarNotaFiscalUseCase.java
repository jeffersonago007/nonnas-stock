package com.nonnas.operations.application.notafiscal;

import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.operations.domain.NotaFiscalId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarNotaFiscalUseCase {

    private final NotaFiscalRepository repository;

    public BuscarNotaFiscalUseCase(NotaFiscalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotaFiscal execute(UUID id) {
        return repository.findById(NotaFiscalId.of(id))
                .orElseThrow(() -> new NotFoundException("NotaFiscal", id));
    }
}
