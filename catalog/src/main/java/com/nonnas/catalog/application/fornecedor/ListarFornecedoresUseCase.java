package com.nonnas.catalog.application.fornecedor;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.Fornecedor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarFornecedoresUseCase {

    private final FornecedorRepository repository;

    public ListarFornecedoresUseCase(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
