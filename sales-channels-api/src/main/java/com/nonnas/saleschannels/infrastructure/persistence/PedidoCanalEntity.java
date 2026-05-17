package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import com.nonnas.web.crypto.CamposSensiveisConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos_canais")
public class PedidoCanalEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "canal_tipo", nullable = false, length = 40, updatable = false) private CanalTipo canalTipo;
    @Column(name = "pedido_externo_id", nullable = false, length = 120, updatable = false) private String pedidoExternoId;
    @Column(name = "display_id", length = 80) private String displayId;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "credencial_id", nullable = false, updatable = false) private UUID credencialId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private StatusPedidoCanal status;
    @Column(name = "valor_total", nullable = false, precision = 20, scale = 4) private BigDecimal valorTotal;
    @Column(name = "taxa_entrega", nullable = false, precision = 20, scale = 4) private BigDecimal taxaEntrega = BigDecimal.ZERO;
    @Column(name = "taxa_servico", nullable = false, precision = 20, scale = 4) private BigDecimal taxaServico = BigDecimal.ZERO;
    @Column(name = "valor_liquido", nullable = false, precision = 20, scale = 4) private BigDecimal valorLiquido = BigDecimal.ZERO;
    @Column(nullable = false, length = 3) private String moeda;
    // PII LGPD — cifrado em repouso via AES-256-GCM (T-QOL-03).
    // length aumentado para acomodar ciphertext base64 (~88 chars p/ nome curto).
    @Convert(converter = CamposSensiveisConverter.class)
    @Column(name = "cliente_nome", length = 500) private String clienteNome;
    @Convert(converter = CamposSensiveisConverter.class)
    @Column(name = "cliente_telefone", length = 500) private String clienteTelefone;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_canonico_json", nullable = false, columnDefinition = "jsonb") private String payloadCanonicoJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_bruto_json", columnDefinition = "jsonb") private String payloadBrutoJson;
    @Column(name = "movimentacao_id") private UUID movimentacaoId;
    @Column(name = "erro_processamento", columnDefinition = "text") private String erroProcessamento;
    @Column(name = "recebido_em", nullable = false, updatable = false) private Instant recebidoEm;
    @Column(name = "processado_em") private Instant processadoEm;
    @Column(name = "concluido_em") private Instant concluidoEm;
    @Column(name = "cancelado_em") private Instant canceladoEm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_canal_id", nullable = false)
    @OrderBy("sequencia ASC")
    private List<ItemPedidoCanalEntity> itens = new ArrayList<>();

    public PedidoCanalEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public CanalTipo getCanalTipo() { return canalTipo; } public void setCanalTipo(CanalTipo v) { this.canalTipo = v; }
    public String getPedidoExternoId() { return pedidoExternoId; } public void setPedidoExternoId(String v) { this.pedidoExternoId = v; }
    public String getDisplayId() { return displayId; } public void setDisplayId(String v) { this.displayId = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public UUID getCredencialId() { return credencialId; } public void setCredencialId(UUID v) { this.credencialId = v; }
    public StatusPedidoCanal getStatus() { return status; } public void setStatus(StatusPedidoCanal v) { this.status = v; }
    public BigDecimal getValorTotal() { return valorTotal; } public void setValorTotal(BigDecimal v) { this.valorTotal = v; }
    public BigDecimal getTaxaEntrega() { return taxaEntrega; } public void setTaxaEntrega(BigDecimal v) { this.taxaEntrega = v; }
    public BigDecimal getTaxaServico() { return taxaServico; } public void setTaxaServico(BigDecimal v) { this.taxaServico = v; }
    public BigDecimal getValorLiquido() { return valorLiquido; } public void setValorLiquido(BigDecimal v) { this.valorLiquido = v; }
    public String getMoeda() { return moeda; } public void setMoeda(String v) { this.moeda = v; }
    public String getClienteNome() { return clienteNome; } public void setClienteNome(String v) { this.clienteNome = v; }
    public String getClienteTelefone() { return clienteTelefone; } public void setClienteTelefone(String v) { this.clienteTelefone = v; }
    public String getPayloadCanonicoJson() { return payloadCanonicoJson; } public void setPayloadCanonicoJson(String v) { this.payloadCanonicoJson = v; }
    public String getPayloadBrutoJson() { return payloadBrutoJson; } public void setPayloadBrutoJson(String v) { this.payloadBrutoJson = v; }
    public UUID getMovimentacaoId() { return movimentacaoId; } public void setMovimentacaoId(UUID v) { this.movimentacaoId = v; }
    public String getErroProcessamento() { return erroProcessamento; } public void setErroProcessamento(String v) { this.erroProcessamento = v; }
    public Instant getRecebidoEm() { return recebidoEm; } public void setRecebidoEm(Instant v) { this.recebidoEm = v; }
    public Instant getProcessadoEm() { return processadoEm; } public void setProcessadoEm(Instant v) { this.processadoEm = v; }
    public Instant getConcluidoEm() { return concluidoEm; } public void setConcluidoEm(Instant v) { this.concluidoEm = v; }
    public Instant getCanceladoEm() { return canceladoEm; } public void setCanceladoEm(Instant v) { this.canceladoEm = v; }
    public List<ItemPedidoCanalEntity> getItens() { return itens; } public void setItens(List<ItemPedidoCanalEntity> v) { this.itens = v; }
}
