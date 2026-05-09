package com.nonnas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios_2fa")
public class Usuario2faEntity {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "secret_cifrado", nullable = false, columnDefinition = "text")
    private String secretCifrado;

    @Column(name = "backup_codes_hash", nullable = false, columnDefinition = "text[]")
    @JdbcTypeCode(Types.ARRAY)
    private String[] backupCodesHash = new String[0];

    @Column(name = "confirmado", nullable = false)
    private boolean confirmado;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Usuario2faEntity() {}

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID v) { this.usuarioId = v; }
    public String getSecretCifrado() { return secretCifrado; }
    public void setSecretCifrado(String v) { this.secretCifrado = v; }
    public String[] getBackupCodesHash() { return backupCodesHash; }
    public void setBackupCodesHash(String[] v) { this.backupCodesHash = v != null ? v : new String[0]; }
    public boolean isConfirmado() { return confirmado; }
    public void setConfirmado(boolean v) { this.confirmado = v; }
    public Instant getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(Instant v) { this.confirmadoEm = v; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant v) { this.criadoEm = v; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant v) { this.atualizadoEm = v; }
}
