package com.nonnas.identity.infrastructure.security;

import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.UsuarioId;

import java.util.Optional;

/**
 * Identity principal placed in Spring Security context after JWT validation.
 * Carries the minimal data the rest of the app may need without re-querying
 * the DB on every request.
 */
public record AuthenticatedPrincipal(
        UsuarioId usuarioId,
        String email,
        Perfil perfil,
        FilialId filialId
) {
    public Optional<FilialId> filialIdOpt() {
        return Optional.ofNullable(filialId);
    }
}
