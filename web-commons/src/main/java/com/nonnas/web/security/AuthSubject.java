package com.nonnas.web.security;

import java.util.UUID;

/**
 * Visão mínima do principal autenticado que módulos sem dependência de
 * {@code identity} podem consumir via {@link SecurityScope}. Identity provê
 * a implementação concreta ({@code AuthenticatedPrincipal}).
 */
public interface AuthSubject {

    UUID userId();

    /** Nome do {@code Perfil}: ADMIN, GERENTE, OPERADOR, CONSULTA. */
    String perfilName();

    /** Filial vinculada ao usuário; {@code null} apenas para ADMIN. */
    UUID filialIdOrNull();

    default boolean isAdmin() {
        return "ADMIN".equals(perfilName());
    }
}
