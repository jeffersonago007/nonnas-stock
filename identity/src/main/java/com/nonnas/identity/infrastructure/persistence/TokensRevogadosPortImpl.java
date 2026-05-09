package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.ports.TokensRevogadosPort;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Repository
class TokensRevogadosPortImpl implements TokensRevogadosPort {

    private final SpringDataTokenRevogadoRepository jpa;
    private final Clock clock;

    TokensRevogadosPortImpl(SpringDataTokenRevogadoRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void revogar(String jti, UUID usuarioId, Instant expiraEm, Motivo motivo) {
        if (jpa.existsByJti(jti)) return; // idempotente — logout duplicado não duplica linha
        jpa.save(new TokenRevogadoEntity(jti, usuarioId, clock.instant(), expiraEm, motivo.name()));
    }

    @Override
    public boolean estaRevogado(String jti) {
        return jpa.existsByJti(jti);
    }
}
