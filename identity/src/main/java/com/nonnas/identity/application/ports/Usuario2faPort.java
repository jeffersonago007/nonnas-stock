package com.nonnas.identity.application.ports;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Usuario2faPort {

    record Snapshot(
            UUID usuarioId,
            String secretCifrado,
            List<String> backupCodesHash,
            boolean confirmado,
            Instant confirmadoEm
    ) {}

    void salvarSetup(UUID usuarioId, String secretCifrado);

    void confirmar(UUID usuarioId, List<String> backupCodesHash, Instant confirmadoEm);

    Optional<Snapshot> findByUsuarioId(UUID usuarioId);

    boolean isConfirmado(UUID usuarioId);

    void desativar(UUID usuarioId);
}
