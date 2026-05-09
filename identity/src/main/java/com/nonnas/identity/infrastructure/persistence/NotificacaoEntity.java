package com.nonnas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacoes_usuario")
public class NotificacaoEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "tipo", nullable = false, length = 64)
    private String tipo;

    @Column(name = "prioridade", nullable = false, length = 16)
    private String prioridade;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "mensagem", nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(name = "link_acao", length = 512)
    private String linkAcao;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "canais_destino", nullable = false)
    private String canaisDestino;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "lida_em")
    private Instant lidaEm;

    @Column(name = "arquivada_em")
    private Instant arquivadaEm;

    public NotificacaoEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID v) { this.usuarioId = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String v) { this.prioridade = v; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String v) { this.titulo = v; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String v) { this.mensagem = v; }
    public String getLinkAcao() { return linkAcao; }
    public void setLinkAcao(String v) { this.linkAcao = v; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String v) { this.metadata = v; }
    public String getCanaisDestino() { return canaisDestino; }
    public void setCanaisDestino(String v) { this.canaisDestino = v; }
    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant v) { this.criadaEm = v; }
    public Instant getLidaEm() { return lidaEm; }
    public void setLidaEm(Instant v) { this.lidaEm = v; }
    public Instant getArquivadaEm() { return arquivadaEm; }
    public void setArquivadaEm(Instant v) { this.arquivadaEm = v; }
}
