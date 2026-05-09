package com.nonnas.catalog.application.insumo;

import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtualizarInsumoUseCase {

    private final InsumoRepository repository;
    private final Clock clock;

    public AtualizarInsumoUseCase(InsumoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Insumo execute(UUID id, String novoNome) {
        Insumo insumo = repository.findById(InsumoId.of(id))
                .orElseThrow(() -> new NotFoundException("Insumo", id));
        insumo.renomear(novoNome, clock.instant());
        return repository.save(insumo);
    }
}
