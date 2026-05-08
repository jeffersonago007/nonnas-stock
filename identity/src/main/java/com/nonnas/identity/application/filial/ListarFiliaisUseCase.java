package com.nonnas.identity.application.filial;

import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ListarFiliaisUseCase {

    private final FilialRepository repository;

    public ListarFiliaisUseCase(FilialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Filial> execute(UUID empresaIdFilter, int page, int size) {
        return Optional.ofNullable(empresaIdFilter)
                .map(EmpresaId::of)
                .map(id -> repository.findByEmpresa(id, page, size))
                .orElseGet(() -> repository.findAll(page, size));
    }
}
