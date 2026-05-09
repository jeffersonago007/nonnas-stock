package com.nonnas.identity.application.lgpd;

import com.nonnas.identity.application.audit.AuditEvent;
import com.nonnas.identity.application.audit.AuditLogService;
import com.nonnas.identity.application.featureflags.FeatureFlag;
import com.nonnas.identity.application.featureflags.FeatureFlagService;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Endpoints LGPD do {@code /api/v1/lgpd/*}.
 *
 * <p>O escopo aqui é mínimo viável de auditoria (master doc 13.5):
 * <ul>
 *   <li>{@code meusDados}: snapshot dos campos pessoais — fonte da
 *       portabilidade Art. 18 IV.</li>
 *   <li>{@code corrigir}: atualiza nome e/ou e-mail (Art. 18 III).</li>
 *   <li>{@code excluir}: anonimização imediata (Art. 18 VI). Mantém o
 *       PK para integridade referencial em movimentações; o nome vira
 *       "Usuário Anonimizado" e o e-mail um placeholder com hash do id.</li>
 * </ul>
 *
 * <p>Trilha completa de cada operação fica em {@code audit_log}.
 */
@Service
public class LgpdService {

    private final UsuarioRepository usuarios;
    private final AuditLogService auditLog;
    private final FeatureFlagService featureFlags;
    private final Clock clock;

    public LgpdService(UsuarioRepository usuarios, AuditLogService auditLog,
                       FeatureFlagService featureFlags, Clock clock) {
        this.usuarios = usuarios;
        this.auditLog = auditLog;
        this.featureFlags = featureFlags;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DadosPessoais meusDados(UUID usuarioId) {
        Usuario u = buscar(usuarioId);
        auditLog.registrarTentativaLogin(AuditEvent.Types.LGPD_DADOS_SOLICITADOS, u, null, null, null);
        return new DadosPessoais(
                u.id().value(),
                u.nome(),
                u.email().value(),
                u.perfil().name(),
                u.filialId().map(f -> f.value()).orElse(null),
                u.createdAt(),
                u.ativo());
    }

    @Transactional
    public DadosPessoais corrigir(UUID usuarioId, String novoNome, String novoEmail) {
        Usuario u = buscar(usuarioId);
        Instant agora = clock.instant();
        if (novoNome != null && !novoNome.isBlank()) {
            u.renomear(novoNome, agora);
        }
        if (novoEmail != null && !novoEmail.isBlank()) {
            u.alterarEmail(Email.of(novoEmail), agora);
        }
        usuarios.save(u);
        auditLog.registrarTentativaLogin(AuditEvent.Types.LGPD_CORRECAO, u, null, null,
                "{\"campos\":\"nome,email\"}");
        return meusDados(usuarioId);
    }

    /**
     * Anonimização imediata: nome vira "Usuário Anonimizado", e-mail vira
     * {@code anonimizado-<id-curto>@nonnas.local} (não reutilizável). Usuário
     * é desativado pra impedir login.
     */
    @Transactional
    public void excluir(UUID usuarioId) {
        // T18 — POC de feature flag. Quando off, a anonimização é bloqueada
        // (kill-switch operacional pra emergências). Padrão: TRUE em prod.
        if (!featureFlags.isAtiva(FeatureFlag.Chaves.LGPD_EXCLUSAO_ATIVADA)) {
            throw new BusinessRuleException(
                    ErrorCode.CONFLICT,
                    "Exclusão LGPD temporariamente indisponível. Tente novamente em algumas horas ou contate o DPO.");
        }
        Usuario u = buscar(usuarioId);
        Instant agora = clock.instant();
        String idCurto = usuarioId.toString().substring(0, 8);
        u.renomear("Usuário Anonimizado", agora);
        u.alterarEmail(Email.of("anonimizado-" + idCurto + "@nonnas.local"), agora);
        u.desativar(agora);
        usuarios.save(u);
        auditLog.registrarTentativaLogin(AuditEvent.Types.LGPD_EXCLUSAO, u, null, null, null);
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.findById(UsuarioId.of(usuarioId))
                .orElseThrow(() -> new NotFoundException("Usuário", usuarioId));
    }

    public record DadosPessoais(
            UUID id,
            String nome,
            String email,
            String perfil,
            UUID filialId,
            Instant criadoEm,
            boolean ativo
    ) {}
}
