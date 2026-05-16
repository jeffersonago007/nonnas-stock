package com.nonnas.saleschannels.infrastructure.schedule;

import com.nonnas.saleschannels.application.opendelivery.EventoBruto;
import com.nonnas.saleschannels.application.ports.CanalAdapter;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.infrastructure.config.SalesChannelsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Faz polling periódico nos canais configurados, salvando eventos novos
 * em {@code eventos_canais} (idempotência por UNIQUE no schema).
 *
 * <p>Habilitado por {@code nonnas.canais.polling.enabled=true}. Em test
 * ({@code application-test.yml}), {@code spring.task.scheduling.enabled=false}
 * já mantém este scheduler off.
 *
 * <p>O processamento dos eventos (criar PedidoCanal, baixar estoque,
 * confirmar no canal) é feito em T-CANAL-04 ({@code ProcessarPedidoCanalUseCase}).
 * Por ora, este scheduler só persiste os eventos brutos.
 */
@Component
public class CanalPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(CanalPollingScheduler.class);

    private final Map<CanalTipo, CanalAdapter> adapters;
    private final EventoCanalRepository eventos;
    private final SalesChannelsProperties props;
    private final Clock clock;

    public CanalPollingScheduler(List<CanalAdapter> adaptersList,
                                  EventoCanalRepository eventos,
                                  SalesChannelsProperties props,
                                  Clock clock) {
        this.adapters = adaptersList.stream()
                .collect(Collectors.toUnmodifiableMap(CanalAdapter::canal, Function.identity()));
        this.eventos = eventos;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Polling agendado. Default 30s — configurável em
     * {@code nonnas.canais.polling.interval}. Só dispara se
     * {@code nonnas.canais.polling.enabled=true}.
     */
    @Scheduled(fixedDelayString = "${nonnas.canais.polling.interval:30s}")
    public void poll() {
        if (!props.polling().enabled()) {
            return;
        }
        adapters.keySet().forEach(this::pollCanal);
    }

    /**
     * Dispara polling sob demanda. Usado pelo endpoint
     * {@code POST /api/v1/canais/{tipo}/poll-now} e por testes.
     * Retorna a quantidade de eventos NOVOS persistidos (não conta
     * duplicados rejeitados pela idempotência).
     */
    public int pollCanal(CanalTipo canal) {
        CanalAdapter adapter = adapters.get(canal);
        if (adapter == null) {
            log.warn("Nenhum adapter registrado para canal {}", canal);
            return 0;
        }

        List<EventoBruto> brutos;
        try {
            brutos = adapter.consumirEventos(props.polling().batchSize());
        } catch (Exception e) {
            log.warn("Falha consumindo eventos do canal {}: {}", canal, e.getMessage());
            return 0;
        }

        int novos = 0;
        for (EventoBruto bruto : brutos) {
            EventoCanal evento = EventoCanal.recebido(
                    canal,
                    bruto.eventIdExterno(),
                    bruto.tipoEvento(),
                    bruto.pedidoExternoId(),
                    Optional.ofNullable(bruto.payloadJson()).orElse("{}"),
                    clock.instant());
            if (eventos.salvarSeNovo(evento).isPresent()) {
                novos++;
            }
        }

        if (novos > 0) {
            log.info("Polling {}: {} eventos novos persistidos", canal, novos);
        }
        return novos;
    }

    /** Acesso pra teste / endpoint manual. */
    public boolean suportaCanal(CanalTipo canal) {
        return adapters.containsKey(canal);
    }
}
