package com.nonnas.catalog.application.unidademedida;

import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.UnidadeMedida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarUnidadesMedidaUseCase {

    private final UnidadeMedidaRepository repository;

    public ListarUnidadesMedidaUseCase(UnidadeMedidaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UnidadeMedida> execute(int page, int size) {
        return repository.findAll(page, size);
    }
}
