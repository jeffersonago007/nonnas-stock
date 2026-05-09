package com.nonnas.e2e.fixtures;

/**
 * Usuário admin criado pelo {@code AdminBootstrap} no startup do backend
 * (ver {@code identity/.../AdminBootstrap}). Senha vem da variável de ambiente
 * em produção; nos testes locais o default do Spring é usado.
 */
public final class TestUsers {

    public static final String ADMIN_EMAIL = "admin@nonnas.com";
    public static final String ADMIN_SENHA = "AdminNonnas2026!";

    private TestUsers() {}
}
