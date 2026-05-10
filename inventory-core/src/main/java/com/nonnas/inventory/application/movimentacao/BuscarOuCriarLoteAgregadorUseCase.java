package com.nonnas.inventory.application.movimentacao;

import com.nonnas.inventory.application.ports.LoteRepository;
import com.nonnas.inventory.domain.Lote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final LoteRepository loteRepo;
    private final Clock clock;

    public BuscarOuCriarLoteAgregadorUseCase(LoteRepository loteRepo, Clock clock) {
        this.loteRepo = loteRepo;
        this.clock = clock;
    }

    @Transactional
    public Lote execute(UUID insumoId) {
        return loteRepo.findAgregadorByInsumo(insumoId)
                .orElseGet(() -> loteRepo.save(Lote.novoAgregador(insumoId, clock.instant())));
    }
}
