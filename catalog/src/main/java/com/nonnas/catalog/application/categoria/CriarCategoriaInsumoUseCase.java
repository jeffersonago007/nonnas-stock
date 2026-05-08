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
public class CriarCategoriaInsumoUseCase {

    private final CategoriaInsumoRepository repository;
    private final Clock clock;

    public CriarCategoriaInsumoUseCase(CategoriaInsumoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CategoriaInsumo execute(String nome, UUID categoriaPaiId) {
        CategoriaInsumoId paiId = null;
        if (categoriaPaiId != null) {
            paiId = CategoriaInsumoId.of(categoriaPaiId);
            if (repository.findById(paiId).isEmpty()) {
                throw new NotFoundException("Categoria pai", categoriaPaiId);
            }
        }
        return repository.save(CategoriaInsumo.nova(nome, paiId, clock.instant()));
    }
}
