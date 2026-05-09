package com.nonnas.recipes.application.ficha;

import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ItemFichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Edição de ficha técnica = nova versão. Sequência (mesma transação):
 * <ol>
 *   <li>Carrega ficha vigente do produto.</li>
 *   <li>Chama {@link FichaTecnica#editar(List, Instant)}, que muta a vigente
 *       (ativa=false, vigente_ate=agora) e devolve a nova ficha.</li>
 *   <li>Salva a vigente atualizada (saveAndFlush força UPDATE primeiro,
 *       evitando colisão no partial unique index com {@code ativa=true}).</li>
 *   <li>Salva a nova ficha (INSERT).</li>
 * </ol>
 *
 * Falha em qualquer passo rola back tudo.
 */
@Service
public class AtualizarFichaTecnicaUseCase {

    private final FichaTecnicaRepository fichaRepo;
    private final Clock clock;

    public AtualizarFichaTecnicaUseCase(FichaTecnicaRepository fichaRepo, Clock clock) {
        this.fichaRepo = fichaRepo;
        this.clock = clock;
    }

    @Transactional
    public FichaTecnica execute(Comando cmd) {
        ProdutoVendavelId produtoId = ProdutoVendavelId.of(cmd.produtoVendavelId);
        FichaTecnica vigente = fichaRepo.findVigentePorProduto(produtoId)
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + cmd.produtoVendavelId + " não encontrada"));

        List<ItemFichaTecnica> novosItens = cmd.itens.stream()
                .map(i -> ItemFichaTecnica.novo(i.insumoId, i.unidadeId, i.quantidade))
                .toList();

        FichaTecnica nova = vigente.editar(novosItens, clock.instant());
        fichaRepo.save(vigente);  // UPDATE old (ativa=false) — saveAndFlush no impl
        return fichaRepo.save(nova);  // INSERT new (ativa=true)
    }

    public record Comando(UUID produtoVendavelId, List<ItemEntrada> itens) {}

    public record ItemEntrada(UUID insumoId, UUID unidadeId, BigDecimal quantidade) {}
}
