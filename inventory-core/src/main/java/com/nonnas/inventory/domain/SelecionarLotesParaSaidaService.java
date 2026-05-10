package com.nonnas.inventory.domain;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Estratégia de seleção de lotes para saída — decide entre AGREGADOR
 * (alocação única) e FEFO (algoritmo original) com base na presença ou não
 * do lote agregador para o insumo.
 *
 * <p>Polimorfismo via dado, não via tipo: {@link LoteRepository#findAgregadorByInsumo}
 * é a chave da decisão. Se existe agregador → toda a quantidade vai pra ele
 * (saldo pode ficar negativo). Se não existe → delega para
 * {@link SelecionarLotesPorFefoService}.
 *
 * <p>O insumo {@code controla_validade} não é consultado aqui — esse flag
 * vive em catalog e este módulo (inventory-core) não importa catalog. O
 * estado do lote agregador no DB é a fonte de verdade: se foi criado pelo
 * {@code BuscarOuCriarLoteAgregadorUseCase} no caminho de entrada, é porque
 * o regime do insumo é não-rastreado.
 */
public final class SelecionarLotesParaSaidaService {

    private final LoteRepository loteRepo;
    private final SaldoLoteRepository saldoRepo;
    private final SelecionarLotesPorFefoService fefo;

    public SelecionarLotesParaSaidaService(LoteRepository loteRepo,
                                           SaldoLoteRepository saldoRepo,
                                           SelecionarLotesPorFefoService fefo) {
        this.loteRepo = Objects.requireNonNull(loteRepo);
        this.saldoRepo = Objects.requireNonNull(saldoRepo);
        this.fefo = Objects.requireNonNull(fefo);
    }

    public SelecionarLotesPorFefoService.Resultado selecionar(UUID insumoId, UUID filialId,
                                                              BigDecimal quantidadeBase) {
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(filialId);
        Objects.requireNonNull(quantidadeBase);
        if (quantidadeBase.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }

        Optional<Lote> agregador = loteRepo.findAgregadorByInsumo(insumoId);
        if (agregador.isPresent()) {
            // Toda a quantidade vai pro agregador; gerouNegativo se saldo
            // (lote, filial) atual < quantidadeBase. Saldo negativo é OK no
            // restaurante — invariante "venda não bloqueia" se mantém.
            Lote lote = agregador.get();
            BigDecimal saldoAtual = saldoRepo.findById(lote.id(), filialId)
                    .map(SaldoLote::quantidadeBase)
                    .orElse(BigDecimal.ZERO);
            boolean gerouNegativo = saldoAtual.compareTo(quantidadeBase) < 0;
            var alocacao = new SelecionarLotesPorFefoService.Alocacao(lote.id(), quantidadeBase);
            return new SelecionarLotesPorFefoService.Resultado(
                    List.of(alocacao), quantidadeBase, gerouNegativo);
        }

        // Caminho RASTREADO original — FEFO.
        return fefo.selecionar(insumoId, filialId, quantidadeBase);
    }
}
