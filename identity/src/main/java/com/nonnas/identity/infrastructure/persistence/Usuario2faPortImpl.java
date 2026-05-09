package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.ports.Usuario2faPort;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class Usuario2faPortImpl implements Usuario2faPort {

    private final SpringDataUsuario2faRepository jpa;
    private final Clock clock;

    Usuario2faPortImpl(SpringDataUsuario2faRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void salvarSetup(UUID usuarioId, String secretCifrado) {
        Instant now = clock.instant();
        Usuario2faEntity entity = jpa.findById(usuarioId).orElseGet(Usuario2faEntity::new);
        if (entity.getCriadoEm() == null) {
            entity.setUsuarioId(usuarioId);
            entity.setCriadoEm(now);
        }
        entity.setSecretCifrado(secretCifrado);
        entity.setBackupCodesHash(new String[0]);
        entity.setConfirmado(false);
        entity.setConfirmadoEm(null);
        entity.setAtualizadoEm(now);
        jpa.save(entity);
    }

    @Override
    public void confirmar(UUID usuarioId, List<String> backupCodesHash, Instant confirmadoEm) {
        Usuario2faEntity entity = jpa.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Setup 2FA não iniciado para usuario " + usuarioId));
        entity.setBackupCodesHash(backupCodesHash.toArray(new String[0]));
        entity.setConfirmado(true);
        entity.setConfirmadoEm(confirmadoEm);
        entity.setAtualizadoEm(confirmadoEm);
        jpa.save(entity);
    }

    @Override
    public Optional<Snapshot> findByUsuarioId(UUID usuarioId) {
        return jpa.findById(usuarioId).map(e -> new Snapshot(
                e.getUsuarioId(),
                e.getSecretCifrado(),
                List.of(e.getBackupCodesHash()),
                e.isConfirmado(),
                e.getConfirmadoEm()));
    }

    @Override
    public boolean isConfirmado(UUID usuarioId) {
        return jpa.existsByUsuarioIdAndConfirmadoTrue(usuarioId);
    }

    @Override
    public void desativar(UUID usuarioId) {
        jpa.deleteById(usuarioId);
    }
}
