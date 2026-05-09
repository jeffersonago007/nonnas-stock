package com.nonnas.alerts.infrastructure.listener;

import com.nonnas.alerts.domain.AvaliadorAlertasService;
import com.nonnas.inventory.application.events.MovimentacaoCriadaEvent;
import com.nonnas.inventory.domain.ItemMovimentacao;
import com.nonnas.inventory.domain.Movimentacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Avalia alertas após cada movimentação. Roda em
 * {@code AFTER_COMMIT} de propósito: o saldo materializado já está
 * atualizado quando começamos, e qualquer falha aqui não rola back a
 * movimentação. Alertas são observabilidade, não invariante de negócio.
 *
 * <p>Para cada {@code (insumo, filial)} afetado pela movimentação,
 * dispara avaliação de estoque (RUPTURA, ESTOQUE_MINIMO_*). Para cada
 * lote afetado, avalia auto-resolução de alertas de vencimento (lote
 * que zerou).
 */
@Component
public class MovimentacaoAlertaListener {

    private static final Logger log = LoggerFactory.getLogger(MovimentacaoAlertaListener.class);

    private final AvaliadorAlertasService avaliador;

    public MovimentacaoAlertaListener(AvaliadorAlertasService avaliador) {
        this.avaliador = avaliador;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMovimentacaoCriada(MovimentacaoCriadaEvent ev) {
        Movimentacao mov = ev.movimentacao();
        try {
            // Pares (insumo, filial) únicos da movimentação
            Map<UUID, UUID> insumoFilial = new LinkedHashMap<>();
            Set<UUID> loteIds = new HashSet<>();
            for (ItemMovimentacao it : mov.itens()) {
                insumoFilial.putIfAbsent(it.insumoId(), mov.filialId());
                loteIds.add(it.loteId().value());
            }
            insumoFilial.forEach((insumo, filial) -> avaliador.avaliarEstoque(insumo, filial));
            avaliador.avaliarLotesVencimento(List.copyOf(loteIds), mov.filialId());
        } catch (RuntimeException e) {
            log.warn("Falha avaliando alertas para movimentação {}: {}", mov.id().value(), e.getMessage(), e);
        }
    }
}
