package com.nonnas.catalog.application.fornecedor;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarFornecedorUseCase {

    private final FornecedorRepository repository;

    public BuscarFornecedorUseCase(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Fornecedor execute(UUID id) {
        return repository.findById(FornecedorId.of(id))
                .orElseThrow(() -> new NotFoundException("Fornecedor", id));
    }
}
