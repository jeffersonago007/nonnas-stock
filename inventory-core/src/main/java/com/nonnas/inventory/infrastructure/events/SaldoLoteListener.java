package com.nonnas.inventory.infrastructure.events;

import com.nonnas.inventory.application.events.MovimentacaoCriadaEvent;
import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.inventory.domain.SaldoLote;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Atualiza o saldo materializado em {@code saldos_lotes} a cada
 * movimentação criada. Roda na <em>mesma transação</em> que persistiu a
 * movimentação (Propagation.MANDATORY) — se o save da mov falha, o saldo
 * não é atualizado, e vice-versa.
 *
 * <p>Para movimentações de entrada, soma quantidade base ao saldo do lote
 * naquela filial. Para saídas, subtrai. Quando o saldo é negativo (caso
 * permitido), persiste valor negativo — o job de conciliação detecta.
 */
@Component
public class SaldoLoteListener {

    private final SaldoLoteRepository saldoRepo;
    private final Clock clock;

    public SaldoLoteListener(SaldoLoteRepository saldoRepo, Clock clock) {
        this.saldoRepo = saldoRepo;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(MovimentacaoCriadaEvent event) {
        var mov = event.movimentacao();
        var agora = clock.instant();
        for (var item : mov.itens()) {
            SaldoLote atual = saldoRepo.findById(item.loteId(), mov.filialId())
                    .orElseGet(() -> SaldoLote.zero(item.loteId(), mov.filialId(), agora));
            SaldoLote novo = mov.tipo().isEntrada()
                    ? atual.acrescentar(item.quantidadeBase(), agora)
                    : atual.subtrair(item.quantidadeBase(), agora);
            saldoRepo.save(novo);
        }
    }
}
