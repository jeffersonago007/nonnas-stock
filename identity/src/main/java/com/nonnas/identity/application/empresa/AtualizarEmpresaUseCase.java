package com.nonnas.identity.application.empresa;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.RazaoSocial;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtualizarEmpresaUseCase {

    private final EmpresaRepository repository;
    private final Clock clock;

    public AtualizarEmpresaUseCase(EmpresaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Empresa execute(UUID id, String novaRazaoSocial) {
        Empresa empresa = repository.findById(EmpresaId.of(id))
                .orElseThrow(() -> new NotFoundException("Empresa", id));
        empresa.renomear(RazaoSocial.of(novaRazaoSocial), clock.instant());
        return repository.save(empresa);
    }
}
