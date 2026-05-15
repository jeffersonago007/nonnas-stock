package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.TipoEventoCanal;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_canais")
public class EventoCanalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "canal_tipo", nullable = false, length = 40, updatable = false) private CanalTipo canalTipo;
    @Column(name = "event_id_externo", nullable = false, length = 120, updatable = false) private String eventIdExterno;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_evento", nullable = false, length = 40, updatable = false) private TipoEventoCanal tipoEvento;
    @Column(name = "pedido_externo_id", length = 120) private String pedidoExternoId;
    @Column(name = "pedido_canal_id") private UUID pedidoCanalId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb") private String payloadJson;
    @Column(name = "recebido_em", nullable = false, updatable = false) private Instant recebidoEm;
    @Column(name = "acknowledged_em") private Instant acknowledgedEm;
    @Column(name = "processado_em") private Instant processadoEm;
    @Column(columnDefinition = "text") private String erro;

    public EventoCanalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public CanalTipo getCanalTipo() { return canalTipo; } public void setCanalTipo(CanalTipo v) { this.canalTipo = v; }
    public String getEventIdExterno() { return eventIdExterno; } public void setEventIdExterno(String v) { this.eventIdExterno = v; }
    public TipoEventoCanal getTipoEvento() { return tipoEvento; } public void setTipoEvento(TipoEventoCanal v) { this.tipoEvento = v; }
    public String getPedidoExternoId() { return pedidoExternoId; } public void setPedidoExternoId(String v) { this.pedidoExternoId = v; }
    public UUID getPedidoCanalId() { return pedidoCanalId; } public void setPedidoCanalId(UUID v) { this.pedidoCanalId = v; }
    public String getPayloadJson() { return payloadJson; } public void setPayloadJson(String v) { this.payloadJson = v; }
    public Instant getRecebidoEm() { return recebidoEm; } public void setRecebidoEm(Instant v) { this.recebidoEm = v; }
    public Instant getAcknowledgedEm() { return acknowledgedEm; } public void setAcknowledgedEm(Instant v) { this.acknowledgedEm = v; }
    public Instant getProcessadoEm() { return processadoEm; } public void setProcessadoEm(Instant v) { this.processadoEm = v; }
    public String getErro() { return erro; } public void setErro(String v) { this.erro = v; }
}
