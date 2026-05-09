package com.nonnas.catalog.application.insumo;

import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarInsumoUseCase {

    private final InsumoRepository repository;

    public BuscarInsumoUseCase(InsumoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Insumo execute(UUID id) {
        return repository.findById(InsumoId.of(id))
                .orElseThrow(() -> new NotFoundException("Insumo", id));
    }
}
