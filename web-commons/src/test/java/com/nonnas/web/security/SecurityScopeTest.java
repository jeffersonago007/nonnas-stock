package com.nonnas.web.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobertura unitária do utilitário central de escopagem. Usa o
 * {@link SecurityContextHolder} real (limpo em {@code @AfterEach}) com um
 * {@link AuthSubject} fake — sem Spring Boot, sem MockMvc.
 */
class SecurityScopeTest {

    private static final UUID FILIAL_A = UUID.randomUUID();
    private static final UUID FILIAL_B = UUID.randomUUID();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static AuthSubject subject(String perfil, UUID filial) {
        return new AuthSubject() {
            @Override public UUID userId() { return UUID.randomUUID(); }
            @Override public String perfilName() { return perfil; }
            @Override public UUID filialIdOrNull() { return filial; }
        };
    }

    private static void autenticar(AuthSubject subject) {
        var auth = new UsernamePasswordAuthenticationToken(subject, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + subject.perfilName())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ---------- isAdmin ----------

    @Test
    void isAdminTrueParaPerfilAdmin() {
        autenticar(subject("ADMIN", null));
        assertThat(SecurityScope.isAdmin()).isTrue();
    }

    @Test
    void isAdminFalseParaNaoAdmin() {
        autenticar(subject("GERENTE", FILIAL_A));
        assertThat(SecurityScope.isAdmin()).isFalse();
    }

    @Test
    void semAutenticacaoLancaAccessDenied() {
        assertThatThrownBy(SecurityScope::isAdmin)
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- currentFilialId ----------

    @Test
    void currentFilialIdVazioParaAdminSemFilial() {
        autenticar(subject("ADMIN", null));
        assertThat(SecurityScope.currentFilialId()).isEmpty();
    }

    @Test
    void currentFilialIdRetornaFilialDoUsuario() {
        autenticar(subject("OPERADOR", FILIAL_A));
        assertThat(SecurityScope.currentFilialId()).contains(FILIAL_A);
    }

    // ---------- assertCanAccess ----------

    @Test
    void adminAcessaQualquerFilial() {
        autenticar(subject("ADMIN", null));
        SecurityScope.assertCanAccess(FILIAL_A);
        SecurityScope.assertCanAccess(FILIAL_B);
    }

    @Test
    void naoAdminAcessaPropriaFilial() {
        autenticar(subject("OPERADOR", FILIAL_A));
        SecurityScope.assertCanAccess(FILIAL_A);
    }

    @Test
    void naoAdminFalhaAoAcessarOutraFilial() {
        autenticar(subject("OPERADOR", FILIAL_A));
        assertThatThrownBy(() -> SecurityScope.assertCanAccess(FILIAL_B))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("filial");
    }

    @Test
    void naoAdminComFilialIdNullFalha() {
        autenticar(subject("GERENTE", FILIAL_A));
        assertThatThrownBy(() -> SecurityScope.assertCanAccess(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void naoAdminSemFilialVinculadaFalha() {
        // Cenário defensivo — em produção a constraint V024 impede chegar nesse estado.
        autenticar(subject("OPERADOR", null));
        assertThatThrownBy(() -> SecurityScope.assertCanAccess(FILIAL_A))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("sem filial");
    }

    // ---------- resolveFilialId ----------

    @Test
    void adminResolvePassaValorRequisitado() {
        autenticar(subject("ADMIN", null));
        assertThat(SecurityScope.resolveFilialId(FILIAL_A)).isEqualTo(FILIAL_A);
    }

    @Test
    void adminResolveAceitaNullParaTodas() {
        autenticar(subject("ADMIN", null));
        assertThat(SecurityScope.resolveFilialId(null)).isNull();
    }

    @Test
    void naoAdminResolveIgnoraRequisitadoEUsaPropria() {
        // Anti-IDOR: mesmo que o cliente envie FILIAL_B, devolvemos FILIAL_A.
        autenticar(subject("OPERADOR", FILIAL_A));
        assertThat(SecurityScope.resolveFilialId(FILIAL_B)).isEqualTo(FILIAL_A);
    }

    @Test
    void naoAdminResolveComNullUsaPropria() {
        autenticar(subject("GERENTE", FILIAL_A));
        assertThat(SecurityScope.resolveFilialId(null)).isEqualTo(FILIAL_A);
    }

    // ---------- requireFilialId ----------

    @Test
    void requireFilialIdAdminSemValorFalha() {
        autenticar(subject("ADMIN", null));
        assertThatThrownBy(() -> SecurityScope.requireFilialId(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireFilialIdAdminComValorPassa() {
        autenticar(subject("ADMIN", null));
        assertThat(SecurityScope.requireFilialId(FILIAL_A)).isEqualTo(FILIAL_A);
    }

    @Test
    void requireFilialIdNaoAdminUsaPropria() {
        autenticar(subject("OPERADOR", FILIAL_A));
        assertThat(SecurityScope.requireFilialId(FILIAL_B)).isEqualTo(FILIAL_A);
    }
}
