package com.nonnas.catalog.application.categoria;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.domain.CategoriaInsumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarCategoriasInsumoUseCase {

    private final CategoriaInsumoRepository repository;

    public ListarCategoriasInsumoUseCase(CategoriaInsumoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaInsumo> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
