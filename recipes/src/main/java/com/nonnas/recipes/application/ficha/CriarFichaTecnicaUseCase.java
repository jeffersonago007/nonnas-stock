package com.nonnas.recipes.application.ficha;

import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Cria a ficha técnica versão 1 de um produto vendável. Recusa se já
 * existir ficha vigente — nesse caso, use {@code AtualizarFichaTecnicaUseCase}
 * para gerar uma nova versão.
 */
@Service
public class CriarFichaTecnicaUseCase {

    private final FichaTecnicaRepository fichaRepo;
    private final ProdutoVendavelRepository produtoRepo;
    private final Clock clock;

    public CriarFichaTecnicaUseCase(FichaTecnicaRepository fichaRepo,
                                    ProdutoVendavelRepository produtoRepo, Clock clock) {
        this.fichaRepo = fichaRepo;
        this.produtoRepo = produtoRepo;
        this.clock = clock;
    }

    @Transactional
    public FichaTecnica execute(Comando cmd) {
        ProdutoVendavelId produtoId = ProdutoVendavelId.of(cmd.produtoVendavelId);
        if (produtoRepo.findById(produtoId).isEmpty()) {
            throw new NotFoundException("Produto vendável", cmd.produtoVendavelId);
        }
        if (fichaRepo.findVigentePorProduto(produtoId).isPresent()) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe ficha vigente para o produto — use atualização para gerar nova versão");
        }

        List<ItemFichaTecnica> itens = cmd.itens.stream()
                .map(i -> ItemFichaTecnica.novo(i.insumoId, i.unidadeId, i.quantidade))
                .toList();
        FichaTecnica ficha = FichaTecnica.nova(produtoId, 1, itens, clock.instant());
        return fichaRepo.save(ficha);
    }

    public record Comando(UUID produtoVendavelId, List<ItemEntrada> itens) {}

    public record ItemEntrada(UUID insumoId, UUID unidadeId, BigDecimal quantidade) {}
}
