package com.nonnas.saleschannels.infrastructure.schedule;

import com.nonnas.saleschannels.application.ProcessarPedidoCanalUseCase;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.infrastructure.config.SalesChannelsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Consome eventos pendentes (não processados) e dispara o pipeline de
 * baixa de estoque + confirmação no canal via {@link ProcessarPedidoCanalUseCase}.
 *
 * <p>Habilitado por {@code nonnas.canais.processar.enabled=true} —
 * independente do scheduler de polling. O endpoint manual
 * {@code POST /api/v1/canais/processar-pendentes} (ADMIN/GERENTE) continua
 * funcionando como retry pela UI, mesmo com o scheduler desligado.
 *
 * <p>O loop captura exceções por evento — uma falha individual não
 * interrompe o batch (o erro já fica gravado no evento via use case).
 */
@Component
public class ProcessarPedidosScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessarPedidosScheduler.class);

    /** UUID determinístico para autoria de movimentação criada pelo scheduler. */
    static final UUID USUARIO_SISTEMA_SCHEDULER =
            UUID.nameUUIDFromBytes("nonnas-canal-scheduler".getBytes());

    private final EventoCanalRepository eventos;
    private final ProcessarPedidoCanalUseCase processarUseCase;
    private final SalesChannelsProperties props;

    public ProcessarPedidosScheduler(EventoCanalRepository eventos,
                                      ProcessarPedidoCanalUseCase processarUseCase,
                                      SalesChannelsProperties props) {
        this.eventos = eventos;
        this.processarUseCase = processarUseCase;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${nonnas.canais.processar.interval:60s}")
    public void processar() {
        if (!props.processar().enabled()) {
            return;
        }
        processarBatch();
    }

    /**
     * Processa um batch — exposto para testes e para ser potencialmente
     * reusado por um endpoint manual no futuro (hoje o endpoint
     * {@code /processar-pendentes} chama o use case diretamente).
     */
    public int processarBatch() {
        List<EventoCanal> pendentes = eventos.listarNaoProcessados(props.processar().batchSize());
        if (pendentes.isEmpty()) {
            return 0;
        }

        int sucesso = 0;
        int falha = 0;
        for (EventoCanal e : pendentes) {
            try {
                processarUseCase.processarEventoNaoProcessado(e.id().value(), USUARIO_SISTEMA_SCHEDULER);
                sucesso++;
            } catch (RuntimeException ex) {
                falha++;
                log.warn("Falha processando evento {}: {}", e.id().value(), ex.getMessage());
            }
        }
        log.info("Scheduler processar: {} sucesso, {} falha (batch {})", sucesso, falha, pendentes.size());
        return sucesso;
    }
}
