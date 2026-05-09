package com.nonnas.recipes.application.venda;

import com.nonnas.inventory.application.movimentacao.RegistrarSaidaMultiItemUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.recipes.application.ports.FichaTecnicaRepository;
import com.nonnas.recipes.domain.FichaTecnica;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Registra venda simulada: busca a ficha vigente, multiplica cada item da
 * receita pela quantidade vendida e delega para {@link RegistrarSaidaMultiItemUseCase}
 * — uma única movimentação SAIDA_VENDA com todos os insumos baixados via FEFO.
 *
 * <p>Snapshot por referência: {@code documento_origem_tipo='FICHA_TECNICA'} e
 * {@code documento_origem_id} aponta para a versão vigente no momento da venda.
 * Edições posteriores da ficha não alteram o histórico.
 */
@Service
public class RegistrarVendaSimuladaUseCase {

    private static final String DOC_ORIGEM_TIPO = "FICHA_TECNICA";

    private final FichaTecnicaRepository fichaRepo;
    private final RegistrarSaidaMultiItemUseCase saidaMulti;

    public RegistrarVendaSimuladaUseCase(FichaTecnicaRepository fichaRepo,
                                         RegistrarSaidaMultiItemUseCase saidaMulti) {
        this.fichaRepo = fichaRepo;
        this.saidaMulti = saidaMulti;
    }

    @Transactional
    public Movimentacao execute(Comando cmd) {
        if (cmd.quantidadeVendida == null || cmd.quantidadeVendida.signum() <= 0) {
            throw new ValidationException("Quantidade vendida deve ser positiva");
        }

        FichaTecnica vigente = fichaRepo.findVigentePorProduto(ProdutoVendavelId.of(cmd.produtoVendavelId))
                .orElseThrow(() -> new NotFoundException(
                        "Ficha técnica vigente para o produto " + cmd.produtoVendavelId + " não encontrada"));

        List<RegistrarSaidaMultiItemUseCase.ItemSaida> itens = vigente.itens().stream()
                .map(i -> new RegistrarSaidaMultiItemUseCase.ItemSaida(
                        i.insumoId(),
                        i.unidadeId(),
                        i.quantidade().multiply(cmd.quantidadeVendida)))
                .toList();

        var saidaCmd = new RegistrarSaidaMultiItemUseCase.Comando(
                cmd.filialId, cmd.usuarioId, TipoMovimentacao.SAIDA_VENDA,
                DOC_ORIGEM_TIPO, vigente.id().value(),
                cmd.observacao, itens);

        return saidaMulti.execute(saidaCmd);
    }

    public record Comando(
            UUID produtoVendavelId,
            UUID filialId,
            UUID usuarioId,
            BigDecimal quantidadeVendida,
            String observacao
    ) {}
}
