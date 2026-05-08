package com.nonnas.catalog.application.insumo;

import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Insumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarInsumosUseCase {

    private final InsumoRepository repository;

    public ListarInsumosUseCase(InsumoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Insumo> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
