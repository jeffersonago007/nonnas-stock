package com.nonnas.recipes.application.produto;

import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CriarProdutoVendavelUseCase {

    private final ProdutoVendavelRepository repo;
    private final Clock clock;

    public CriarProdutoVendavelUseCase(ProdutoVendavelRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public ProdutoVendavel execute(Comando cmd) {
        if (repo.existsByCodigo(cmd.codigo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe produto vendável com código " + cmd.codigo);
        }
        ProdutoVendavel novo = ProdutoVendavel.novo(cmd.codigo, cmd.nome, cmd.categoria, clock.instant());
        return repo.save(novo);
    }

    public record Comando(String codigo, String nome, String categoria) {}
}
