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
public class AtualizarProdutoVendavelUseCase {

    private final ProdutoVendavelRepository repository;
    private final Clock clock;

    public AtualizarProdutoVendavelUseCase(ProdutoVendavelRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ProdutoVendavel execute(UUID id, String novoNome, String novaCategoria) {
        ProdutoVendavel produto = repository.findById(ProdutoVendavelId.of(id))
                .orElseThrow(() -> new NotFoundException("Produto vendável", id));
        var agora = clock.instant();
        produto.renomear(novoNome, agora);
        produto.recategorizar(novaCategoria, agora);
        return repository.save(produto);
    }
}
