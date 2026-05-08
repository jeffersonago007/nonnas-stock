package com.nonnas.inventory.infrastructure.persistence;

import com.nonnas.inventory.domain.TipoMovimentacao;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes")
public class MovimentacaoEntity {

    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "filial_id", nullable = false, updatable = false) private UUID filialId;
    @Column(name = "usuario_id", nullable = false, updatable = false) private UUID usuarioId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40, updatable = false) private TipoMovimentacao tipo;
    @Column(name = "data_movimentacao", nullable = false, updatable = false) private Instant dataMovimentacao;
    @Column(name = "documento_origem_tipo", length = 40, updatable = false) private String documentoOrigemTipo;
    @Column(name = "documento_origem_id", updatable = false) private UUID documentoOrigemId;
    @Column(updatable = false) private String observacao;
    @Column(name = "gerou_negativo", nullable = false, updatable = false) private boolean gerouNegativo;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public MovimentacaoEntity() {}

    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFilialId() { return filialId; } public void setFilialId(UUID v) { this.filialId = v; }
    public UUID getUsuarioId() { return usuarioId; } public void setUsuarioId(UUID v) { this.usuarioId = v; }
    public TipoMovimentacao getTipo() { return tipo; } public void setTipo(TipoMovimentacao v) { this.tipo = v; }
    public Instant getDataMovimentacao() { return dataMovimentacao; } public void setDataMovimentacao(Instant v) { this.dataMovimentacao = v; }
    public String getDocumentoOrigemTipo() { return documentoOrigemTipo; } public void setDocumentoOrigemTipo(String v) { this.documentoOrigemTipo = v; }
    public UUID getDocumentoOrigemId() { return documentoOrigemId; } public void setDocumentoOrigemId(UUID v) { this.documentoOrigemId = v; }
    public String getObservacao() { return observacao; } public void setObservacao(String v) { this.observacao = v; }
    public boolean isGerouNegativo() { return gerouNegativo; } public void setGerouNegativo(boolean v) { this.gerouNegativo = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
