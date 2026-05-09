package com.nonnas.identity.application.notifications;

import com.nonnas.identity.application.ports.NotificacaoRepository;
import com.nonnas.sharedkernel.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when; // import retido — outros testes usam when() direto

@ExtendWith(MockitoExtension.class)
class NotificacaoInternaServiceTest {

    @Mock NotificacaoRepository repository;
    @Mock CanalNotificacao canalInterno;
    @Mock CanalNotificacao canalEmail;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-09T15:00:00Z"), ZoneOffset.UTC);

    private NotificacaoInternaService service;
    private UUID usuarioId;

    @BeforeEach
    void setup() {
        // lenient: alguns testes não interagem com os canais e Mockito strict
        // protestaria com UnnecessaryStubbingException.
        lenient().when(canalInterno.nome()).thenReturn("INTERNO");
        lenient().when(canalEmail.nome()).thenReturn("EMAIL");
        service = new NotificacaoInternaService(repository,
                List.of(canalInterno, canalEmail), clock);
        usuarioId = UUID.randomUUID();
    }

    @Test
    void criarDespachaParaCanaisListadosEmCanaisDestino() {
        NovaNotificacao nova = new NovaNotificacao(
                usuarioId, "ALERTA_DISPARADO",
                Notificacao.Prioridade.CRITICA,
                "Ruptura", "saldo zerado", "/alertas", null, "INTERNO");

        service.criar(nova);

        verify(canalInterno).despachar(nova);
        verify(canalEmail, never()).despachar(any());
    }

    @Test
    void criarDespachaParaMultiplosCanais() {
        NovaNotificacao nova = new NovaNotificacao(
                usuarioId, "ALERTA_DISPARADO",
                Notificacao.Prioridade.CRITICA,
                "Ruptura", "saldo zerado", "/alertas", null, "INTERNO,EMAIL");

        service.criar(nova);

        verify(canalInterno).despachar(nova);
        verify(canalEmail).despachar(nova);
    }

    @Test
    void contarNaoLidasDelegaParaRepositorio() {
        when(repository.countNaoLidas(usuarioId)).thenReturn(7L);

        assertThat(service.contarNaoLidas(usuarioId)).isEqualTo(7L);
    }

    @Test
    void marcarLidaValidaOwnership() {
        UUID notifId = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        Notificacao alheia = new Notificacao(notifId, outroUsuario, "X",
                Notificacao.Prioridade.INFO, "t", "m", null, null, "INTERNO",
                Instant.now(), null, null);
        when(repository.findById(notifId)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> service.marcarLida(notifId, usuarioId))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).marcarLida(any(), any());
    }

    @Test
    void marcarLidaSucessoChamaRepositorio() {
        UUID notifId = UUID.randomUUID();
        Notificacao minha = new Notificacao(notifId, usuarioId, "X",
                Notificacao.Prioridade.INFO, "t", "m", null, null, "INTERNO",
                Instant.now(), null, null);
        when(repository.findById(notifId)).thenReturn(Optional.of(minha));

        service.marcarLida(notifId, usuarioId);

        verify(repository).marcarLida(eq(notifId), any());
    }

    @Test
    void marcarTodasLidasDelega() {
        when(repository.marcarTodasLidas(eq(usuarioId), any())).thenReturn(5);

        assertThat(service.marcarTodasLidas(usuarioId)).isEqualTo(5);
        verify(repository, times(1)).marcarTodasLidas(eq(usuarioId), any());
    }

    @Test
    void arquivarValidaOwnership() {
        UUID notifId = UUID.randomUUID();
        UUID outro = UUID.randomUUID();
        Notificacao alheia = new Notificacao(notifId, outro, "X",
                Notificacao.Prioridade.INFO, "t", "m", null, null, "INTERNO",
                Instant.now(), null, null);
        when(repository.findById(notifId)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> service.arquivar(notifId, usuarioId))
                .isInstanceOf(NotFoundException.class);
    }

    private static org.assertj.core.api.AbstractLongAssert<?> assertThat(long value) {
        return org.assertj.core.api.Assertions.assertThat(value);
    }

    private static org.assertj.core.api.AbstractIntegerAssert<?> assertThat(int value) {
        return org.assertj.core.api.Assertions.assertThat(value);
    }
}
