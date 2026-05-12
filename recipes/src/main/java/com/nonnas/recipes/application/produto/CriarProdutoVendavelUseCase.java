package com.nonnas.recipes.application.produto;

import com.nonnas.recipes.application.ports.CatalogQueries;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CriarProdutoVendavelUseCase {

    private final ProdutoVendavelRepository repo;
    private final CatalogQueries catalog;
    private final Clock clock;

    public CriarProdutoVendavelUseCase(ProdutoVendavelRepository repo,
                                       CatalogQueries catalog,
                                       Clock clock) {
        this.repo = repo;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional
    public ProdutoVendavel execute(Comando cmd) {
        if (repo.existsByCodigo(cmd.codigo)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe produto vendável com código " + cmd.codigo);
        }
        TipoProdutoVendavel tipo = cmd.tipo == null ? TipoProdutoVendavel.FABRICADO : cmd.tipo;
        ProdutoVendavel novo = switch (tipo) {
            case FABRICADO -> {
                if (cmd.insumoRevendaId != null) {
                    throw new ValidationException("Produto fabricado não aceita insumo de revenda");
                }
                yield ProdutoVendavel.novoFabricado(cmd.codigo, cmd.nome, cmd.categoria, clock.instant());
            }
            case REVENDA -> {
                if (cmd.insumoRevendaId == null) {
                    throw new ValidationException("Produto de revenda exige insumo vinculado");
                }
                if (catalog.findUnidadeBaseDoInsumo(cmd.insumoRevendaId).isEmpty()) {
                    throw new NotFoundException("Insumo", cmd.insumoRevendaId);
                }
                yield ProdutoVendavel.novoRevenda(cmd.codigo, cmd.nome, cmd.categoria,
                        cmd.insumoRevendaId, clock.instant());
            }
        };
        return repo.save(novo);
    }

    public record Comando(String codigo, String nome, String categoria,
                          TipoProdutoVendavel tipo, UUID insumoRevendaId) {
        public Comando(String codigo, String nome, String categoria) {
            this(codigo, nome, categoria, TipoProdutoVendavel.FABRICADO, null);
        }
    }
}
