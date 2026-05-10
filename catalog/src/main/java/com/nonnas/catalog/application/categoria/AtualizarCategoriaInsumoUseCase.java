package com.nonnas.catalog.application.categoria;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.domain.CategoriaInsumo;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AtualizarCategoriaInsumoUseCase {

    private final CategoriaInsumoRepository repository;
    private final Clock clock;

    public AtualizarCategoriaInsumoUseCase(CategoriaInsumoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CategoriaInsumo execute(UUID id, String novoNome) {
        CategoriaInsumo categoria = repository.findById(CategoriaInsumoId.of(id))
                .orElseThrow(() -> new NotFoundException("CategoriaInsumo", id));
        categoria.renomear(novoNome, clock.instant());
        return repository.save(categoria);
    }
}
