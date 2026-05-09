package com.nonnas.identity.application.lgpd;

import com.nonnas.identity.application.audit.AuditLogService;
import com.nonnas.identity.application.featureflags.FeatureFlagService;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.SenhaHash;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock AuditLogService auditLog;
    @Mock FeatureFlagService featureFlags;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC);

    @InjectMocks LgpdService service;

    private Usuario usuario;
    private UUID id;

    @BeforeEach
    void setup() {
        // Substitui o relógio do @InjectMocks (que é mockado por default).
        // FeatureFlag default ativa — tests explícitos sobrescrevem quando precisam.
        org.mockito.Mockito.lenient().when(featureFlags.isAtiva(any())).thenReturn(true);
        service = new LgpdService(usuarios, auditLog, featureFlags, clock);
        id = UUID.randomUUID();
        usuario = new Usuario(
                UsuarioId.of(id),
                null,
                "Original",
                Email.of("original@nonnas.com"),
                SenhaHash.of("$2a$12$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV01"),
                Perfil.ADMIN,
                true,
                0,
                null,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(usuarios.findById(UsuarioId.of(id))).thenReturn(Optional.of(usuario));
    }

    @Test
    void corrigirAtualizaNomeEEmailEAuditaEvento() {
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LgpdService.DadosPessoais resultado = service.corrigir(id, "Nome Novo", "novo@nonnas.com");

        assertThat(resultado.nome()).isEqualTo("Nome Novo");
        assertThat(resultado.email()).isEqualTo("novo@nonnas.com");
        // 2 audit events: LGPD_CORRECAO + LGPD_DADOS_SOLICITADOS (chamada interna a meusDados).
        verify(auditLog, times(2)).registrarTentativaLogin(any(), any(), any(), any(), any());
    }

    @Test
    void corrigirIgnoraCamposVaziosOuNulos() {
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LgpdService.DadosPessoais resultado = service.corrigir(id, "", null);

        // Nada mudou — nome/email originais permanecem.
        assertThat(resultado.nome()).isEqualTo("Original");
        assertThat(resultado.email()).isEqualTo("original@nonnas.com");
    }

    @Test
    void excluirAnonimizaENotificaAuditoria() {
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.excluir(id);

        // Confirma que o usuário foi anonimizado in-place
        assertThat(usuario.nome()).isEqualTo("Usuário Anonimizado");
        assertThat(usuario.email().value()).startsWith("anonimizado-");
        assertThat(usuario.email().value()).endsWith("@nonnas.local");
        assertThat(usuario.ativo()).isFalse();

        // Auditoria registrou a exclusão
        verify(auditLog).registrarTentativaLogin(
                argThat(t -> "LGPD_EXCLUSAO".equals(t)), any(), any(), any(), any());
    }

    @Test
    void meusDadosRetornaSnapshotEAudita() {
        LgpdService.DadosPessoais resultado = service.meusDados(id);

        assertThat(resultado.id()).isEqualTo(id);
        assertThat(resultado.email()).isEqualTo("original@nonnas.com");
        assertThat(resultado.perfil()).isEqualTo("ADMIN");
        assertThat(resultado.ativo()).isTrue();
        verify(auditLog).registrarTentativaLogin(
                argThat(t -> "LGPD_DADOS_SOLICITADOS".equals(t)), any(), any(), any(), any());
    }
}
