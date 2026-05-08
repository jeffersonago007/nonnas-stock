package com.nonnas.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static final SenhaHash SENHA = SenhaHash.of("$2a$12$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRS");
    private static final Email EMAIL = Email.of("u@n.com");
    private static final Instant T0 = Instant.parse("2026-05-08T12:00:00Z");

    private Usuario novoUsuario() {
        return Usuario.novo(null, "Adm", EMAIL, SENHA, Perfil.ADMIN, T0);
    }

    @Test
    void novoUsuarioVemAtivoSemBloqueio() {
        Usuario u = novoUsuario();
        assertThat(u.ativo()).isTrue();
        assertThat(u.travada()).isFalse();
        assertThat(u.tentativasFalhas()).isZero();
        assertThat(u.bloqueadoAte()).isEmpty();
        assertThat(u.podeLogar(T0)).isTrue();
    }

    @Test
    void registrarTresFalhasBloqueia15Min() {
        Usuario u = novoUsuario();
        u.registrarLoginFalho(T0);
        u.registrarLoginFalho(T0);
        u.registrarLoginFalho(T0);
        assertThat(u.tentativasFalhas()).isEqualTo(3);
        assertThat(u.estaBloqueado(T0)).isTrue();
        assertThat(u.bloqueadoAte()).hasValueSatisfying(b ->
                assertThat(b).isEqualTo(T0.plusSeconds(15 * 60)));
    }

    @Test
    void registrarCincoFalhasEscalonaPara1H() {
        Usuario u = novoUsuario();
        for (int i = 0; i < 5; i++) u.registrarLoginFalho(T0);
        assertThat(u.tentativasFalhas()).isEqualTo(5);
        assertThat(u.bloqueadoAte()).hasValueSatisfying(b ->
                assertThat(b).isEqualTo(T0.plusSeconds(60 * 60)));
    }

    @Test
    void dezFalhasTravamConta() {
        Usuario u = novoUsuario();
        for (int i = 0; i < 10; i++) u.registrarLoginFalho(T0);
        assertThat(u.travada()).isTrue();
        assertThat(u.estaBloqueado(T0)).isTrue();
    }

    @Test
    void loginSucessoZeraContadorMasNaoDestrava() {
        Usuario u = novoUsuario();
        for (int i = 0; i < 10; i++) u.registrarLoginFalho(T0);
        u.registrarLoginSucesso(T0); // hipotético — em prod travada bloqueia antes
        assertThat(u.tentativasFalhas()).isZero();
        assertThat(u.bloqueadoAte()).isEmpty();
        assertThat(u.travada()).isTrue(); // só ADMIN libera
    }

    @Test
    void liberarRestauraEstado() {
        Usuario u = novoUsuario();
        for (int i = 0; i < 10; i++) u.registrarLoginFalho(T0);
        u.liberar(T0);
        assertThat(u.travada()).isFalse();
        assertThat(u.tentativasFalhas()).isZero();
        assertThat(u.bloqueadoAte()).isEmpty();
        assertThat(u.podeLogar(T0)).isTrue();
    }

    @Test
    void bloqueioExpiraQuandoTempoPassa() {
        Usuario u = novoUsuario();
        u.registrarLoginFalho(T0);
        u.registrarLoginFalho(T0);
        u.registrarLoginFalho(T0);
        Instant futuro = T0.plusSeconds(16 * 60);
        assertThat(u.estaBloqueado(futuro)).isFalse();
    }
}
