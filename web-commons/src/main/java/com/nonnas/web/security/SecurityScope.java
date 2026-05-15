package com.nonnas.web.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utilitário de leitura do contexto de segurança para aplicar escopagem por
 * filial nos controllers/services dos bounded contexts.
 *
 * <p>Regras:
 * <ul>
 *   <li>{@code isAdmin()} — ADMIN tem visão global, ignora filtros de filial.</li>
 *   <li>{@code currentFilialId()} — devolve a filial do principal; vazio para ADMIN.</li>
 *   <li>{@code assertCanAccess(filialId)} — 403 se não-admin tentar acessar filial diferente da sua.</li>
 *   <li>{@code resolveFilialId(requested)} — devolve a filial efetiva: o ADMIN escolhe (pode ser {@code null}),
 *       o não-admin sempre opera dentro da sua filial (ignora {@code requested} para evitar IDOR).</li>
 * </ul>
 *
 * <p>Todas as falhas de autorização lançam {@link AccessDeniedException} e são
 * traduzidas para 403 pelo {@code GlobalExceptionHandler}.
 */
public final class SecurityScope {

    private SecurityScope() {}

    public static AuthSubject current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Sem autenticação no contexto");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthSubject subject) {
            return subject;
        }
        throw new AccessDeniedException("Principal não suporta escopagem por filial");
    }

    public static boolean isAdmin() {
        return current().isAdmin();
    }

    public static Optional<UUID> currentFilialId() {
        return Optional.ofNullable(current().filialIdOrNull());
    }

    /**
     * Garante que o principal pode acessar dados da filial indicada.
     * <ul>
     *   <li>ADMIN: sempre passa.</li>
     *   <li>Não-admin: passa apenas se a filial bate com a do principal.</li>
     * </ul>
     */
    public static void assertCanAccess(UUID filialId) {
        AuthSubject me = current();
        if (me.isAdmin()) {
            return;
        }
        if (filialId == null) {
            throw new AccessDeniedException("filialId é obrigatório para perfis não-ADMIN");
        }
        UUID mine = me.filialIdOrNull();
        if (mine == null) {
            throw new AccessDeniedException("Usuário não-ADMIN sem filial vinculada");
        }
        if (!mine.equals(filialId)) {
            throw new AccessDeniedException("Acesso restrito à própria filial");
        }
    }

    /**
     * Resolve a filial efetiva considerando o perfil:
     * <ul>
     *   <li>ADMIN: respeita o valor informado (pode ser {@code null} = todas).</li>
     *   <li>Não-admin: ignora {@code requested} (anti-IDOR) e força a própria filial.</li>
     * </ul>
     */
    public static UUID resolveFilialId(UUID requested) {
        AuthSubject me = current();
        if (me.isAdmin()) {
            return requested;
        }
        UUID mine = me.filialIdOrNull();
        if (mine == null) {
            throw new AccessDeniedException("Usuário não-ADMIN sem filial vinculada");
        }
        return mine;
    }

    /** Variante que exige uma filial concreta (mesmo para ADMIN). */
    public static UUID requireFilialId(UUID requested) {
        UUID resolved = resolveFilialId(requested);
        if (resolved == null) {
            throw new AccessDeniedException("filialId é obrigatório");
        }
        return resolved;
    }
}
