package com.nonnas.recipes.application.produto;

import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarProdutosVendaveisUseCase {

    private final ProdutoVendavelRepository repository;

    public ListarProdutosVendaveisUseCase(ProdutoVendavelRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoVendavel> execute(String categoria, Boolean ativo, TipoProdutoVendavel tipo,
                                         String q, int page, int size) {
        return repository.findFiltered(categoria, ativo, tipo, q, page, size);
    }
}
