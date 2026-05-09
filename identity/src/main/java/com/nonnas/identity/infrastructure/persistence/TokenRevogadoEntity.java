package com.nonnas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_revogados")
public class TokenRevogadoEntity {

    @Id
    @Column(name = "jti", length = 64)
    private String jti;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "revogado_em", nullable = false)
    private Instant revogadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "motivo", nullable = false, length = 64)
    private String motivo;

    public TokenRevogadoEntity() {}

    public TokenRevogadoEntity(String jti, UUID usuarioId, Instant revogadoEm, Instant expiraEm, String motivo) {
        this.jti = jti;
        this.usuarioId = usuarioId;
        this.revogadoEm = revogadoEm;
        this.expiraEm = expiraEm;
        this.motivo = motivo;
    }
}
