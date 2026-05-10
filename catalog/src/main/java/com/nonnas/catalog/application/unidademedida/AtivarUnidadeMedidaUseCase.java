package com.nonnas.catalog.application.unidademedida;

import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtivarUnidadeMedidaUseCase {

    private final UnidadeMedidaRepository repository;
    private final Clock clock;

    public AtivarUnidadeMedidaUseCase(UnidadeMedidaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public UnidadeMedida execute(UUID id) {
        UnidadeMedida unidade = repository.findById(UnidadeMedidaId.of(id))
                .orElseThrow(() -> new NotFoundException("UnidadeMedida", id));
        unidade.ativar(clock.instant());
        return repository.save(unidade);
    }
}
