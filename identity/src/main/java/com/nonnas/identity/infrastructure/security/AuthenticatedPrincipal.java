package com.nonnas.identity.infrastructure.security;

import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.web.security.AuthSubject;

import java.util.Optional;
import java.util.UUID;

/**
 * Identity principal placed in Spring Security context after JWT validation.
 * Carries the minimal data the rest of the app may need without re-querying
 * the DB on every request.
 *
 * <p>Implementa {@link AuthSubject} para que {@code SecurityScope} (web-commons)
 * consiga aplicar escopagem por filial sem que cada bounded context dependa de
 * {@code identity}.
 */
public record AuthenticatedPrincipal(
        UsuarioId usuarioId,
        String email,
        Perfil perfil,
        FilialId filialId
) implements AuthSubject {

    public Optional<FilialId> filialIdOpt() {
        return Optional.ofNullable(filialId);
    }

    @Override
    public UUID userId() {
        return usuarioId.value();
    }

    @Override
    public String perfilName() {
        return perfil.name();
    }

    @Override
    public UUID filialIdOrNull() {
        return filialId == null ? null : filialId.value();
    }
}
