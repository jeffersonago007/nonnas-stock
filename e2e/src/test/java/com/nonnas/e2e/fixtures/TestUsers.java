package com.nonnas.e2e.fixtures;

/**
 * Usuário admin criado pelo {@code AdminBootstrap} no startup do backend
 * (ver {@code identity/.../AdminBootstrap}). Senha vem da variável de ambiente
 * em produção; nos testes locais o default do Spring é usado.
 */
public final class TestUsers {

    public static final String ADMIN_EMAIL = "admin@nonnas.com";
    public static final String ADMIN_SENHA = "AdminNonnas2026!";

    // Usuários por perfil criados pelo RbacMenuE2ETest (idempotente).
    // Senha satisfaz a política: 10+ chars, 1 letra, 1 número, 1 especial.
    public static final String SENHA_PADRAO_E2E = "Senha-E2E-123!";

    public static final String GERENTE_EMAIL = "rbac.gerente@e2e.com";
    public static final String GERENTE_NOME = "Gerente RBAC E2E";

    public static final String OPERADOR_EMAIL = "rbac.operador@e2e.com";
    public static final String OPERADOR_NOME = "Operador RBAC E2E";

    public static final String CONSULTA_EMAIL = "rbac.consulta@e2e.com";
    public static final String CONSULTA_NOME = "Consulta RBAC E2E";

    private TestUsers() {}
}
