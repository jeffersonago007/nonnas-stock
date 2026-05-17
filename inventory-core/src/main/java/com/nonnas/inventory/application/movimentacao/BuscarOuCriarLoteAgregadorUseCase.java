package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.Lote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.UUID;

/**
 * Garante que existe um lote AGREGADOR para o insumo. Se já existe,
 * retorna; senão cria. Idempotente — chamadas concorrentes são protegidas
 * pelo unique partial index em {@code lotes(insumo_id) WHERE tipo='AGREGADOR'}
 * (V020 inventory-core), então o pior caso é uma transação perder a corrida
 * e receber {@code DataIntegrityViolationException} no commit. Tratamos isso
 * com um segundo lookup como fallback (lazy retry) — o Spring envolve em
 * runtime exception, propagamos sem tentar capturar e re-lookup aqui pra
 * manter a transação simples; o caller (lançamento de NF) opera dentro de
 * uma transação maior e qualquer conflito força rollback completo, o que é
 * aceitável para um caminho que só dispara uma vez por insumo.
 *
 * <p>Use case fica em {@code inventory-core} para preservar o invariante
 * arquitetural: nenhum módulo externo manipula {@code Lote} sem passar por
 * um use case (regra ArchUnit em T-LOT-09).
 */
@Service
public class BuscarOuCriarLoteAgregadorUseCase {

    /** Precisão suficiente pra evitar erro acumulado em milhares de entradas
     *  (mais que NUMERIC(20,4) da coluna). Half-up é o arredondamento fiscal. */
    private static final MathContext CUSTO_MEDIO_MC = new MathContext(20, RoundingMode.HALF_UP);

    private final LoteRepository loteRepo;
    private final SaldoLoteRepository saldoRepo;
    private final Clock clock;

    public BuscarOuCriarLoteAgregadorUseCase(LoteRepository loteRepo, SaldoLoteRepository saldoRepo, Clock clock) {
        this.loteRepo = loteRepo;
        this.saldoRepo = saldoRepo;
        this.clock = clock;
    }

    @Transactional
    public Lote execute(UUID insumoId) {
        return loteRepo.findAgregadorByInsumo(insumoId)
                .orElseGet(() -> loteRepo.save(Lote.novoAgregador(insumoId, clock.instant())));
    }

    /**
     * Variante usada no caminho de entrada (T-CMV-01): além de buscar/criar
     * o lote, recalcula o custo médio ponderado:
     *
     *   novo_custo = (saldo_anterior × custo_anterior + qtd_entrada × custo_entrada)
     *              / (saldo_anterior + qtd_entrada)
     *
     * O saldo anterior é a soma cross-filial; uma única matriz por insumo é a
     * decisão adotada (ver STATUS T-CMV-01). Quando o lote acaba de ser
     * criado (saldo=0, custo=0), o resultado degenera em {@code custo_entrada} —
     * primeira entrada estampa o custo direto.
     */
    @Transactional
    public Lote executeComCusto(UUID insumoId, BigDecimal qtdEntrada, BigDecimal custoUnitarioEntrada) {
        Lote lote = execute(insumoId);
        BigDecimal saldoAnterior = saldoRepo.somarSaldoTotalLote(lote.id());
        BigDecimal custoAnterior = lote.valorUnitario();

        BigDecimal numerador = saldoAnterior.multiply(custoAnterior)
                .add(qtdEntrada.multiply(custoUnitarioEntrada));
        BigDecimal denominador = saldoAnterior.add(qtdEntrada);
        if (denominador.signum() <= 0) {
            return lote; // sem mudança — caller já fez a validação de qtd>0
        }
        BigDecimal novoCustoMedio = numerador.divide(denominador, CUSTO_MEDIO_MC)
                .setScale(4, RoundingMode.HALF_UP);
        if (novoCustoMedio.compareTo(custoAnterior) == 0) {
            return lote;
        }
        return loteRepo.save(lote.comNovoValorUnitarioAgregador(novoCustoMedio));
    }
}
