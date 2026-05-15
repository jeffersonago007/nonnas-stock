package com.nonnas.saleschannels.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Evento bruto recebido do canal — guardado para idempotência (UNIQUE em
 * {@code canalTipo + eventIdExterno} no schema) e auditoria.
 *
 * <p>O campo {@code payloadJson} preserva o payload original do canal:
 * útil quando o mapper não consegue interpretar e precisamos reprocessar
 * manualmente.
 */
public final class EventoCanal {

    private final EventoCanalId id;
    private final CanalTipo canalTipo;
    private final String eventIdExterno;
    private final TipoEventoCanal tipoEvento;
    private final String pedidoExternoId;
    private PedidoCanalId pedidoCanalId;
    private final String payloadJson;
    private final Instant recebidoEm;
    private Instant acknowledgedEm;
    private Instant processadoEm;
    private String erro;

    public EventoCanal(EventoCanalId id, CanalTipo canalTipo, String eventIdExterno,
                       TipoEventoCanal tipoEvento, String pedidoExternoId,
                       PedidoCanalId pedidoCanalId, String payloadJson,
                       Instant recebidoEm, Instant acknowledgedEm,
                       Instant processadoEm, String erro) {
        this.id = Objects.requireNonNull(id);
        this.canalTipo = Objects.requireNonNull(canalTipo);
        this.eventIdExterno = exigir(eventIdExterno, "eventIdExterno");
        this.tipoEvento = Objects.requireNonNull(tipoEvento);
        this.pedidoExternoId = pedidoExternoId;
        this.pedidoCanalId = pedidoCanalId;
        this.payloadJson = exigir(payloadJson, "payloadJson");
        this.recebidoEm = Objects.requireNonNull(recebidoEm);
        this.acknowledgedEm = acknowledgedEm;
        this.processadoEm = processadoEm;
        this.erro = erro;
    }

    public static EventoCanal recebido(CanalTipo canalTipo, String eventIdExterno,
                                       TipoEventoCanal tipoEvento, String pedidoExternoId,
                                       String payloadJson, Instant agora) {
        return new EventoCanal(EventoCanalId.generate(), canalTipo, eventIdExterno,
                tipoEvento, pedidoExternoId, null, payloadJson, agora, null, null, null);
    }

    public void vincularPedido(PedidoCanalId pedidoCanalId) {
        this.pedidoCanalId = Objects.requireNonNull(pedidoCanalId);
    }

    public void marcarAcknowledged(Instant agora) {
        this.acknowledgedEm = agora;
    }

    public void marcarProcessado(Instant agora) {
        this.processadoEm = agora;
        this.erro = null;
    }

    public void marcarErro(String erro, Instant agora) {
        this.processadoEm = agora;
        this.erro = erro;
    }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) {
            throw new ValidationException(campo + " é obrigatório");
        }
        return v;
    }

    public EventoCanalId id() { return id; }
    public CanalTipo canalTipo() { return canalTipo; }
    public String eventIdExterno() { return eventIdExterno; }
    public TipoEventoCanal tipoEvento() { return tipoEvento; }
    public Optional<String> pedidoExternoIdOpt() { return Optional.ofNullable(pedidoExternoId); }
    public Optional<PedidoCanalId> pedidoCanalIdOpt() { return Optional.ofNullable(pedidoCanalId); }
    public String payloadJson() { return payloadJson; }
    public Instant recebidoEm() { return recebidoEm; }
    public Optional<Instant> acknowledgedEmOpt() { return Optional.ofNullable(acknowledgedEm); }
    public Optional<Instant> processadoEmOpt() { return Optional.ofNullable(processadoEm); }
    public Optional<String> erroOpt() { return Optional.ofNullable(erro); }
}
