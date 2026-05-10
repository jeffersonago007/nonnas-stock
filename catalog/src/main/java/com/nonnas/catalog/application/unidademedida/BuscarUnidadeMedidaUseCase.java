package com.nonnas.catalog.application.unidademedida;

import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarUnidadeMedidaUseCase {

    private final UnidadeMedidaRepository repository;

    public BuscarUnidadeMedidaUseCase(UnidadeMedidaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UnidadeMedida execute(UUID id) {
        return repository.findById(UnidadeMedidaId.of(id))
                .orElseThrow(() -> new NotFoundException("UnidadeMedida", id));
    }
}
