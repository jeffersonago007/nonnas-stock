package com.nonnas.identity.application.empresa;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtivarEmpresaUseCase {

    private final EmpresaRepository repository;
    private final Clock clock;

    public AtivarEmpresaUseCase(EmpresaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Empresa execute(UUID id) {
        Empresa empresa = repository.findById(EmpresaId.of(id))
                .orElseThrow(() -> new NotFoundException("Empresa", id));
        empresa.ativar(clock.instant());
        return repository.save(empresa);
    }
}
