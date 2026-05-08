package com.nonnas.identity.application.empresa;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Empresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarEmpresasUseCase {

    private final EmpresaRepository repository;

    public ListarEmpresasUseCase(EmpresaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Empresa> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
