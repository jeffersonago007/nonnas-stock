package com.nonnas.operations.application.notafiscal;

import com.nonnas.inventory.application.movimentacao.RegistrarEntradaMultiItemUseCase;
import com.nonnas.inventory.domain.Movimentacao;
import com.nonnas.inventory.domain.TipoMovimentacao;
import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.FornecedorInsumoDePara;
import com.nonnas.operations.domain.ItemNotaFiscal;
import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.operations.domain.NotaFiscalId;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lança uma nota fiscal já com fornecedor e insumos resolvidos: persiste a
 * {@link NotaFiscal}, dispara entrada de estoque consolidada via
 * {@link RegistrarEntradaMultiItemUseCase} e atualiza o de-para
 * fornecedor↔insumo para acelerar imports futuros.
 *
 * <p>Idempotência por {@code chaveNfe}: lançar a mesma NF-e duas vezes retorna
 * 409 sem efeito colateral. Notas em modo manual (sem chave) não disparam
 * essa proteção — responsabilidade do operador checar duplicidade.
 *
 * <p>Não resolve fornecedor/insumo a partir de descrição. Esse trabalho cabe
 * ao orquestrador (ProcessarNotaFiscalUseCase em {@code nfe-importer}), que
 * pode cruzar fronteiras de bounded contexts.
 */
@Service
public class LancarNotaFiscalUseCase {

    private final NotaFiscalRepository notaRepo;
    private final FornecedorInsumoDeParaRepository deParaRepo;
    private final RegistrarEntradaMultiItemUseCase registrarEntrada;
    private final Clock clock;

    public LancarNotaFiscalUseCase(NotaFiscalRepository notaRepo,
                                   FornecedorInsumoDeParaRepository deParaRepo,
                                   RegistrarEntradaMultiItemUseCase registrarEntrada,
                                   Clock clock) {
        this.notaRepo = notaRepo;
        this.deParaRepo = deParaRepo;
        this.registrarEntrada = registrarEntrada;
        this.clock = clock;
    }

    @Transactional
    public NotaFiscal execute(Comando cmd) {
        validar(cmd);
        if (cmd.chaveNfe != null && !cmd.chaveNfe.isBlank()
                && notaRepo.existsByChaveNfe(cmd.chaveNfe)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Nota fiscal já lançada (chave " + cmd.chaveNfe + ")");
        }

        Instant agora = clock.instant();
        NotaFiscalId notaId = NotaFiscalId.generate();

        // 1) Movimentação ENTRADA_NF multi-item — gera lote por item.
        List<RegistrarEntradaMultiItemUseCase.ItemEntrada> entradas = new ArrayList<>(cmd.itens.size());
        int seq = 0;
        for (Item item : cmd.itens) {
            seq++;
            String numeroLote = item.lote != null && !item.lote.isBlank()
                    ? item.lote
                    : "NF-" + cmd.numero + "-" + seq;
            entradas.add(new RegistrarEntradaMultiItemUseCase.ItemEntrada(
                    item.insumoId,
                    cmd.fornecedorId,
                    notaId.value(),
                    numeroLote,
                    null,
                    item.dataValidade,
                    item.valorUnitario,
                    item.unidadeMedidaId,
                    item.quantidade,
                    item.quantidade));
        }
        var movCmd = new RegistrarEntradaMultiItemUseCase.Comando(
                cmd.filialId, cmd.usuarioId, TipoMovimentacao.ENTRADA_NF,
                "NOTA_FISCAL", notaId.value(),
                cmd.observacao, entradas);
        Movimentacao mov = registrarEntrada.execute(movCmd);

        // 2) Persiste NotaFiscal apontando para a movimentação criada.
        List<ItemNotaFiscal> itensPersistidos = new ArrayList<>(cmd.itens.size());
        for (Item item : cmd.itens) {
            itensPersistidos.add(ItemNotaFiscal.novo(
                    item.insumoId, item.codigoFornecedor, item.descricaoOrigem,
                    item.quantidade, item.unidadeMedidaId,
                    item.valorUnitario, item.valorTotal,
                    item.lote, item.dataValidade));
        }
        NotaFiscal nota = new NotaFiscal(notaId, cmd.fornecedorId, cmd.filialId,
                cmd.numero, cmd.serie, cmd.chaveNfe, cmd.dataEmissao,
                agora, cmd.valorTotal, cmd.observacao, cmd.usuarioId,
                mov.id().value(), itensPersistidos, agora, agora);
        NotaFiscal saved = notaRepo.save(nota);

        // 3) Aprende de-para para itens que vieram com codigoFornecedor.
        atualizarDePara(cmd.fornecedorId, cmd.itens, agora);

        return saved;
    }

    private void atualizarDePara(UUID fornecedorId, List<Item> itens, Instant agora) {
        for (Item item : itens) {
            if (item.codigoFornecedor == null || item.codigoFornecedor.isBlank()) continue;
            var existente = deParaRepo.findByFornecedorAndCodigo(fornecedorId, item.codigoFornecedor);
            if (existente.isPresent()) {
                FornecedorInsumoDePara dp = existente.get();
                dp.marcarComoUsado(agora);
                deParaRepo.save(dp);
            } else {
                deParaRepo.save(FornecedorInsumoDePara.novo(
                        fornecedorId, item.codigoFornecedor, item.insumoId, agora));
            }
        }
    }

    private static void validar(Comando cmd) {
        if (cmd == null) throw new ValidationException("Comando obrigatório");
        if (cmd.itens == null || cmd.itens.isEmpty()) {
            throw new ValidationException("Nota fiscal deve ter ao menos um item");
        }
    }

    public record Comando(
            UUID filialId,
            UUID usuarioId,
            UUID fornecedorId,
            String numero,
            String serie,
            String chaveNfe,
            Instant dataEmissao,
            BigDecimal valorTotal,
            String observacao,
            List<Item> itens
    ) {}

    public record Item(
            UUID insumoId,
            String codigoFornecedor,
            String descricaoOrigem,
            BigDecimal quantidade,
            UUID unidadeMedidaId,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String lote,
            LocalDate dataValidade
    ) {}
}
