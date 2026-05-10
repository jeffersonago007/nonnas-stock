package com.nonnas.identity.application.empresa;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarEmpresaUseCase {

    private final EmpresaRepository repository;

    public BuscarEmpresaUseCase(EmpresaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Empresa execute(UUID id) {
        return repository.findById(EmpresaId.of(id))
                .orElseThrow(() -> new NotFoundException("Empresa", id));
    }
}
