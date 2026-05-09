package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alertas_disparados")
public class AlertaDisparadoEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "config_id", nullable = false, updatable = false) private UUID configId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40, updatable = false) private TipoAlerta tipo;
    @Column(name = "insumo_id", nullable = false, updatable = false) private UUID insumoId;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "lote_id", updatable = false) private UUID loteId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusAlerta status;
    @Column(name = "saldo_no_disparo", precision = 20, scale = 4, updatable = false) private BigDecimal saldoNoDisparo;
    @Column(updatable = false) private String detalhe;
    @Column(name = "data_disparo", nullable = false, updatable = false) private Instant dataDisparo;
    @Column(name = "data_resolucao") private Instant dataResolucao;
    @Column(name = "visualizado_em") private Instant visualizadoEm;
    @Column(name = "visualizado_por") private UUID visualizadoPor;
    @Column(name = "resolvido_por") private UUID resolvidoPor;

    public AlertaDisparadoEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getConfigId() { return configId; } public void setConfigId(UUID v) { this.configId = v; }
    public TipoAlerta getTipo() { return tipo; } public void setTipo(TipoAlerta v) { this.tipo = v; }
    public UUID getInsumoId() { return insumoId; } public void setInsumoId(UUID v) { this.insumoId = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public UUID getLoteId() { return loteId; } public void setLoteId(UUID v) { this.loteId = v; }
    public StatusAlerta getStatus() { return status; } public void setStatus(StatusAlerta v) { this.status = v; }
    public BigDecimal getSaldoNoDisparo() { return saldoNoDisparo; } public void setSaldoNoDisparo(BigDecimal v) { this.saldoNoDisparo = v; }
    public String getDetalhe() { return detalhe; } public void setDetalhe(String v) { this.detalhe = v; }
    public Instant getDataDisparo() { return dataDisparo; } public void setDataDisparo(Instant v) { this.dataDisparo = v; }
    public Instant getDataResolucao() { return dataResolucao; } public void setDataResolucao(Instant v) { this.dataResolucao = v; }
    public Instant getVisualizadoEm() { return visualizadoEm; } public void setVisualizadoEm(Instant v) { this.visualizadoEm = v; }
    public UUID getVisualizadoPor() { return visualizadoPor; } public void setVisualizadoPor(UUID v) { this.visualizadoPor = v; }
    public UUID getResolvidoPor() { return resolvidoPor; } public void setResolvidoPor(UUID v) { this.resolvidoPor = v; }
}
