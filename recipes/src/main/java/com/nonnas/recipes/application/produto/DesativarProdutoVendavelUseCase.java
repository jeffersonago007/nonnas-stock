package com.nonnas.recipes.application.produto;

import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class DesativarProdutoVendavelUseCase {

    private final ProdutoVendavelRepository repository;
    private final Clock clock;

    public DesativarProdutoVendavelUseCase(ProdutoVendavelRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ProdutoVendavel execute(UUID id) {
        ProdutoVendavel produto = repository.findById(ProdutoVendavelId.of(id))
                .orElseThrow(() -> new NotFoundException("Produto vendável", id));
        produto.desativar(clock.instant());
        return repository.save(produto);
    }
}
