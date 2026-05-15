package com.nonnas.identity.domain;

import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void rejeitaCriarOperadorSemFilial() {
        assertThatThrownBy(() ->
                Usuario.novo(null, "Op", EMAIL, SENHA, Perfil.OPERADOR, T0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não-ADMIN");
    }

    @Test
    void rejeitaCriarGerenteSemFilial() {
        assertThatThrownBy(() ->
                Usuario.novo(null, "Ge", EMAIL, SENHA, Perfil.GERENTE, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejeitaCriarConsultaSemFilial() {
        assertThatThrownBy(() ->
                Usuario.novo(null, "Co", EMAIL, SENHA, Perfil.CONSULTA, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aceitaOperadorComFilial() {
        FilialId f = FilialId.of(UUID.randomUUID());
        Usuario u = Usuario.novo(f, "Op", EMAIL, SENHA, Perfil.OPERADOR, T0);
        assertThat(u.filialId()).contains(f);
        assertThat(u.perfil()).isEqualTo(Perfil.OPERADOR);
    }

    @Test
    void alterarPerfilParaNaoAdminSemFilialFalha() {
        Usuario admin = novoUsuario(); // ADMIN sem filial
        assertThatThrownBy(() -> admin.alterarPerfil(Perfil.OPERADOR, T0))
                .isInstanceOf(ValidationException.class);
        assertThat(admin.perfil()).isEqualTo(Perfil.ADMIN); // não muta em falha
    }

    @Test
    void moverParaNullEmNaoAdminFalha() {
        FilialId f = FilialId.of(UUID.randomUUID());
        Usuario op = Usuario.novo(f, "Op", EMAIL, SENHA, Perfil.OPERADOR, T0);
        assertThatThrownBy(() -> op.moverPara(null, T0))
                .isInstanceOf(ValidationException.class);
        assertThat(op.filialId()).contains(f);
    }

    @Test
    void adminPodeFicarSemFilial() {
        Usuario admin = novoUsuario();
        assertThat(admin.filialId()).isEmpty();
        admin.moverPara(null, T0); // no-op válido
        assertThat(admin.filialId()).isEmpty();
    }
}
