package com.nonnas.inventory.domain;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Algoritmo FEFO — First Expired, First Out — exigido por master doc 5.2.
 *
 * <p>Itera lotes do insumo na filial ordenados por data_validade (NULLS
 * LAST) e id, consumindo até completar a quantidade pedida. Pode produzir
 * múltiplas alocações (uma por lote). Se a soma dos saldos não bastar,
 * sinaliza com {@code gerouNegativo = true} e aloca o restante no último
 * lote — saldo fica negativo. Restaurante prefere registrar a venda mesmo
 * sem estoque a bloquear o pedido (regra invariante).
 *
 * <p>Domain service puro. Recebe {@link SaldoLoteRepository} via construtor.
 */
public final class SelecionarLotesPorFefoService {

    private final SaldoLoteRepository saldoRepo;

    public SelecionarLotesPorFefoService(SaldoLoteRepository saldoRepo) {
        this.saldoRepo = Objects.requireNonNull(saldoRepo);
    }

    public Resultado selecionar(UUID insumoId, UUID filialId, BigDecimal quantidadeBase) {
        Objects.requireNonNull(insumoId);
        Objects.requireNonNull(filialId);
        Objects.requireNonNull(quantidadeBase);
        if (quantidadeBase.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }

        List<SaldoLoteRepository.LoteSaldoFefo> disponiveis =
                saldoRepo.findLotesParaSaidaFefo(insumoId, filialId);

        List<Alocacao> alocacoes = new ArrayList<>();
        BigDecimal restante = quantidadeBase;

        for (var l : disponiveis) {
            if (restante.signum() <= 0) break;
            BigDecimal usar = l.saldoBase().min(restante);
            alocacoes.add(new Alocacao(l.loteId(), usar));
            restante = restante.subtract(usar);
        }

        // Se ainda falta quantidade, aloca no último lote (cria saldo negativo)
        // ou cria uma alocação "virtual" se nem houvesse lote.
        boolean gerouNegativo = restante.signum() > 0;
        if (gerouNegativo) {
            if (alocacoes.isEmpty()) {
                // Sem qualquer lote disponível para esse insumo na filial.
                // Isso é cenário extremo — não há nem lote para registrar saída.
                // Retornamos resultado vazio com flag para o use case decidir
                // (tipicamente lança erro de configuração).
                return new Resultado(List.of(), quantidadeBase, true);
            }
            // Adiciona o restante no último lote — fica negativo.
            Alocacao ultima = alocacoes.remove(alocacoes.size() - 1);
            alocacoes.add(new Alocacao(ultima.loteId(), ultima.quantidade().add(restante)));
        }

        return new Resultado(List.copyOf(alocacoes), quantidadeBase, gerouNegativo);
    }

    public record Alocacao(LoteId loteId, BigDecimal quantidade) {}

    public record Resultado(List<Alocacao> alocacoes, BigDecimal quantidadeOriginal, boolean gerouNegativo) {
        public boolean semLotes() { return alocacoes.isEmpty(); }
    }
}
