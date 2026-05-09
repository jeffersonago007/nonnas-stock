package com.nonnas.recipes.application.ficha;

import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListarHistoricoFichasUseCase {

    private final FichaTecnicaRepository repository;

    public ListarHistoricoFichasUseCase(FichaTecnicaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FichaTecnica> execute(UUID produtoId) {
        return repository.findHistoricoPorProduto(ProdutoVendavelId.of(produtoId));
    }
}
