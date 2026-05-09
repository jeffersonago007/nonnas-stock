package com.nonnas.catalog.application.fornecedor;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtivarFornecedorUseCase {

    private final FornecedorRepository repository;
    private final Clock clock;

    public AtivarFornecedorUseCase(FornecedorRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Fornecedor execute(UUID id) {
        Fornecedor fornecedor = repository.findById(FornecedorId.of(id))
                .orElseThrow(() -> new NotFoundException("Fornecedor", id));
        fornecedor.ativar(clock.instant());
        return repository.save(fornecedor);
    }
}
