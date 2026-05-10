package com.nonnas.catalog.application.categoria;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.domain.CategoriaInsumo;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarCategoriaInsumoUseCase {

    private final CategoriaInsumoRepository repository;

    public BuscarCategoriaInsumoUseCase(CategoriaInsumoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CategoriaInsumo execute(UUID id) {
        return repository.findById(CategoriaInsumoId.of(id))
                .orElseThrow(() -> new NotFoundException("CategoriaInsumo", id));
    }
}
