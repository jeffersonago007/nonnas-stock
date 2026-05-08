package com.nonnas.identity.domain;

/**
 * Perfis de acesso. Hierarquia implícita do mais privilegiado ao mais restrito.
 *
 * <ul>
 *   <li>{@code ADMIN} — acesso total, único que cria empresas/filiais e libera contas travadas.</li>
 *   <li>{@code GERENTE} — opera filiais que tutela; aprova transferências e ajustes.</li>
 *   <li>{@code OPERADOR} — lança movimentações na filial onde está vinculado.</li>
 *   <li>{@code CONSULTA} — somente leitura.</li>
 * </ul>
 */
public enum Perfil {
    ADMIN,
    GERENTE,
    OPERADOR,
    CONSULTA;

    public String authority() {
        return "ROLE_" + name();
    }
}
